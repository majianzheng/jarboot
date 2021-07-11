import * as React from "react";
import {Result, Tag} from "antd";
import { getLocale } from 'umi';
import ServerMgrService from "@/services/ServerMgrService";
import CommonNotice from '@/common/CommonNotice';
import {
    SyncOutlined, CaretRightOutlined, ExclamationCircleOutlined, CaretRightFilled, DashboardOutlined,
    PoweroffOutlined, ReloadOutlined, UploadOutlined, LoadingOutlined
} from '@ant-design/icons';
import {JarBootConst} from '@/common/JarBootConst';
import {MsgData, WsManager} from "@/common/WsManager";
import Logger from "@/common/Logger";
import {MSG_EVENT} from "@/common/EventConst";
import {formatMsg} from "@/common/IntlFormat";
import {ServerPubsubImpl} from "@/components/servers/ServerPubsubImpl";
import {PUB_TOPIC, SuperPanel} from "@/components/servers/SuperPanel";
import OneClickButtons from "@/components/servers/OneClickButtons";
import CommonTable from "@/components/table";
import UploadFileModal from "@/components/servers/UploadFileModal";
import StringUtil from "@/common/StringUtil";

interface ServerRunning {
    name: string,
    status: string,
    pid: number
}

const pubsub: PublishSubmit = new ServerPubsubImpl();

const toolButtonStyle = {color: '#1890ff', fontSize: '18px'};
const toolButtonRedStyle = {color: 'red', fontSize: '18px'};
const toolButtonGreenStyle = {color: 'green', fontSize: '18px'};

const notSelectInfo = () => {
    if ('zh-CN' === getLocale()) {
        CommonNotice.info('请点击选择一个服务执行');
    } else {
        CommonNotice.info('Please select one to operate');
    }
};

export default class ServerMgrView extends React.PureComponent {
    state = {loading: false, data: new Array<any>(), uploadVisible: false,
        selectedRowKeys: new Array<any>(),
        selectRows: new Array<any>(), current: '', oneClickLoading: false};
    allServerOut: any = [];
    height = window.innerHeight - 120;
    componentDidMount() {
        this.refreshServerList(true);
        //初始化websocket的事件处理
        WsManager.addMessageHandler(MSG_EVENT.CONSOLE_LINE, this._console);
        WsManager.addMessageHandler(MSG_EVENT.RENDER_JSON, this._renderCmdJsonResult);
        WsManager.addMessageHandler(MSG_EVENT.SERVER_STATUS, this._serverStatusChange);
        WsManager.addMessageHandler(MSG_EVENT.CMD_END, this._commandEnd);
    }

    componentWillUnmount() {
        WsManager.removeMessageHandler(MSG_EVENT.CONSOLE_LINE);
        WsManager.removeMessageHandler(MSG_EVENT.RENDER_JSON);
        WsManager.removeMessageHandler(MSG_EVENT.SERVER_STATUS);
        WsManager.removeMessageHandler(MSG_EVENT.CMD_END);
    }

    private _renderCmdJsonResult = (data: MsgData) => {
        if ('{' !== data.body[0]) {
            //不是json数据时，使用console
            this._console(data);
            return;
        }
        const body = JSON.parse(data.body);
        pubsub.publish(data.server, PUB_TOPIC.RENDER_JSON, body);
    }

    private _serverStatusChange = (data: MsgData) => {
        const server = data.server;
        const status = data.body;
        switch (status) {
            case JarBootConst.MSG_TYPE_START:
                Logger.log(`启动中${server}...`);
                pubsub.publish(server, 'startLoading');
                this._updateServerStatus(server, JarBootConst.STATUS_STARTING);
                break;
            case JarBootConst.MSG_TYPE_STOP:
                Logger.log(`停止中${server}...`);
                pubsub.publish(server, 'startLoading');
                this._updateServerStatus(server, JarBootConst.STATUS_STOPPING);
                break;
            case JarBootConst.MSG_TYPE_START_ERROR:
                Logger.log(`启动失败${server}`);
                CommonNotice.error(`Start ${server} failed!`);
                this._updateServerStatus(server, JarBootConst.STATUS_STOPPED);
                break;
            case JarBootConst.MSG_TYPE_STARTED:
                Logger.log(`启动成功${server}`);
                pubsub.publish(server, 'finishLoading');
                this._updateServerStatus(server, JarBootConst.STATUS_STARTED)
                break;
            case JarBootConst.MSG_TYPE_STOP_ERROR:
                Logger.log(`停止失败${server}`);
                CommonNotice.error(`Stop ${server} failed!`);
                this._updateServerStatus(server, JarBootConst.STATUS_STARTED);
                break;
            case JarBootConst.MSG_TYPE_STOPPED:
                Logger.log(`停止成功${server}`);
                pubsub.publish(server, 'finishLoading');
                this._updateServerStatus(server, JarBootConst.STATUS_STOPPED)
                break;
            case JarBootConst.MSG_TYPE_RESTART:
                Logger.log(`重启成功${server}`);
                pubsub.publish(server, 'finishLoading');
                this._updateServerStatus(server, JarBootConst.STATUS_STARTED)
                break;
            default:
                break;
        }
    };

    private _console = (data: MsgData) => {
        pubsub.publish(data.server, 'appendLine', data.body);
    }

    private _commandEnd = (data: MsgData) => {
        pubsub.publish(data.server, PUB_TOPIC.CMD_END, data.body);
    }

    private _updateServerStatus(server: string, status: string) {
        let data: ServerRunning[] = this.state.data;
        data = data.map((value: ServerRunning) => {
            if (value.name === server) {
                value.status = status;
                value.pid = 0;
            }
            return value;
        });
        this.setState({data});
    }

    private _getTbProps() {
        return {
            columns: [
                {
                    title: formatMsg('NAME'),
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                },
                {
                    title: formatMsg('STATUS'),
                    dataIndex: 'status',
                    key: 'status',
                    ellipsis: true,
                    width: 120,
                    render: (text: string) => ServerMgrView.translateStatus(text),
                },
            ],
            loading: this.state.loading,
            dataSource: this.state.data,
            pagination: false,
            rowKey: 'name',
            size: 'small',
            rowSelection: this._getRowSelection(),
            onRow: this._onRow.bind(this),
            showHeader: true,
            scroll: this.height,
        };
    }
    private static translateStatus(status: string) {
        let tag;
        const s = formatMsg(status);
        switch (status) {
            case JarBootConst.STATUS_STARTED:
                tag = <Tag icon={<CaretRightOutlined/>} color={"success"}>{s}</Tag>
                break;
            case JarBootConst.STATUS_STOPPED:
                tag = <Tag icon={<ExclamationCircleOutlined style={{color: '#f50'}}/>} color={"volcano"}>{s}</Tag>
                break;
            case JarBootConst.STATUS_STARTING:
                tag = <Tag icon={<SyncOutlined spin/>} color={"processing"}>{s}</Tag>
                break;
            case JarBootConst.STATUS_STOPPING:
                tag = <Tag icon={<SyncOutlined spin/>} color={"default"}>{s}</Tag>
                break;
            default:
                tag = <Tag color={"default"}>{s}</Tag>
                break;
        }
        return tag;
    }

    private _getRowSelection() {
        return {
            type: 'checkbox',
            onChange: (selectedRowKeys: any, selectedRows: any) => {
                let current = this.state.current;
                if (!selectedRows || selectedRows.length <= 0) {
                    current = '';
                } else {
                    current = '' === current ? selectedRowKeys[0] : current;
                }
                this.setState({current, selectedRowKeys: selectedRowKeys, selectRows: selectedRows,});
            },
            selectedRowKeys: this.state.selectedRowKeys,
        };
    }

    private _onRow(record: any) {
        return {
            onClick: () => {
                this.setState({
                    selectedRowKeys: [record.name],
                    selectRows: [record],
                    current: record.name,
                });
            },
        };
    }

    private _initAllServerOut(servers: any[]) {
        let allList = [];
        if (servers && servers.length > 0) {
            allList = servers.map(value => value.name);
        }
        allList.push("");
        this.allServerOut = allList;
    }

    private refreshServerList = (init: boolean = false) => {
        this.setState({loading: true});
        ServerMgrService.getServerList((resp: any) => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            const data = resp.result;
            this._initAllServerOut(data);
            if (init) {
                //初始化选中第一个
                let current = '';
                let selectedRowKeys: any = [];
                let selectRows: any = [];
                if (data.length > 0) {
                    const first: any = data[0];
                    current = first.name as string;
                    selectedRowKeys = [first.name];
                    selectRows = [first];
                }
                this.setState({data, current, selectedRowKeys, selectRows});
                return;
            }
            this.setState({data});
        });
    };

    private _finishCallback = (resp: any) => {
        this.setState({loading: false});
        if (0 !== resp.resultCode) {
            CommonNotice.errorFormatted(resp);
        }
    };

    private startServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            notSelectInfo();
            return;
        }
        this.setState({out: "", loading: true});
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.startServer(this.state.selectedRowKeys, this._finishCallback);
    };

    private _clearDisplay = (server: string) => {
        pubsub.publish(server, 'clear');
    };

    private stopServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            notSelectInfo();
            return;
        }
        this.setState({out: "", loading: true});
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.stopServer(this.state.selectedRowKeys, this._finishCallback);
    };
    private restartServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            notSelectInfo();
            return;
        }
        this.setState({out: "", loading: true});
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.restartServer(this.state.selectedRowKeys, this._finishCallback);
    };
    private _getTbBtnProps = () => {
        return [
            {
                name: 'Start',
                key: 'start ',
                icon: <CaretRightFilled style={toolButtonGreenStyle}/>,
                onClick: this.startServer,
            },
            {
                name: 'Stop',
                key: 'stop',
                icon: <PoweroffOutlined style={toolButtonRedStyle}/>,
                onClick: this.stopServer,
            },
            {
                name: 'Restart',
                key: 'restart',
                icon: <ReloadOutlined style={toolButtonStyle}/>,
                onClick: this.restartServer,
            },
            {
                name: 'Refresh',
                key: 'refresh',
                icon: <SyncOutlined style={toolButtonStyle}/>,
                onClick: () => this.refreshServerList(),
            },
            {
                name: 'New & update',
                key: 'upload',
                icon: <UploadOutlined style={toolButtonStyle}/>,
                onClick: this.uploadFile,
            },
            {
                name: 'Dashboard',
                key: 'dashboard',
                icon: <DashboardOutlined style={toolButtonRedStyle}/>,
                onClick: this.dashboardCmd,
            }
        ]
    };

    private uploadFile = () => {
        this.setState({uploadVisible: true});
    };

    private dashboardCmd = () => {
        if (this.state.selectRows?.length < 1 || StringUtil.isEmpty(this.state.current)) {
            notSelectInfo();
            return;
        }
        pubsub.publish(this.state.current, PUB_TOPIC.QUICK_EXEC_CMD, "dashboard");
    };

    private oneClickRestart = () => {
        pubsub.publish(this.state.current, 'appendLine', "Restarting all...");
        this._disableOnClickButton();
        ServerMgrService.oneClickRestart();
    };

    private oneClickStart = () => {
        pubsub.publish(this.state.current, 'appendLine', "Starting all...");
        this._disableOnClickButton();
        ServerMgrService.oneClickStart();
    };

    private oneClickStop = () => {
        pubsub.publish(this.state.current, 'appendLine', "Stopping all...");
        this._disableOnClickButton();
        ServerMgrService.oneClickStop();
    };

    private _disableOnClickButton() {
        if (this.state.oneClickLoading) {
            return;
        }
        this.setState({oneClickLoading: true}, () => {
            const td = setTimeout(() => {
                clearTimeout(td);
                this.setState({oneClickLoading: false});
            }, 5000);
        });
    }

    private onUploadClose = () => {
        this.setState({uploadVisible: false});
        this.refreshServerList();
    };

    render() {
        let tableOption: any = this._getTbProps();
        tableOption.scroll = { y: this.height};
        return (<div>
            <div style={{display: 'flex'}}>
                <div style={{flex: 'inherit', width: '28%'}}>
                    <OneClickButtons loading={this.state.oneClickLoading}
                                     oneClickRestart={this.oneClickRestart}
                                     oneClickStart={this.oneClickStart}
                                     oneClickStop={this.oneClickStop}/>
                    <CommonTable toolbarGap={5} option={tableOption} toolbar={this._getTbBtnProps()} height={this.height}/>
                </div>
                <div style={{flex: 'inherit', width: '72%'}}>
                    {(this.state.loading && 0 == this.allServerOut.length) && <Result icon={<LoadingOutlined/>} title={formatMsg('LOADING')}/>}
                    {this.allServerOut.map((value: any) => (
                        <SuperPanel key={value} server={value} pubsub={pubsub} visible={this.state.current === value}/>
                    ))}
                </div>
            </div>
            {this.state.uploadVisible && <UploadFileModal server={this.state.current}
                             onClose={this.onUploadClose}/>}
        </div>);
    }
}
