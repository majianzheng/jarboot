import * as React from "react";
import styles from "./index.less";
import {Button, Card, Tag, Space, Input} from "antd";
// @ts-ignore
import CommonTable from "../commonTable/CommonTable";
import ServerMgrService from "@/services/ServerMgrService";
import CommonNotice from '@/common/CommonNotice';
import {SyncOutlined, CaretRightOutlined, ExclamationCircleOutlined, CaretRightFilled, EnterOutlined, LoadingOutlined,
    PoweroffOutlined, ReloadOutlined, CloseCircleOutlined} from '@ant-design/icons';
// @ts-ignore
import Console from "@/components/console/Console";
import {JarBootConst} from '@/common/JarBootConst';
import {MsgData, WsManager} from "@/common/WsManager";
import ErrorUtil from '@/common/ErrorUtil';
import Logger from "@/common/Logger";
import {MSG_EVENT} from "@/common/EventConst";
import StringUtil from "@/common/StringUtil";
import { useIntl } from 'umi';
import {memo} from "react";
import {formatMsg} from "@/common/IntlFormat";
import {ServerPubsubImpl} from "@/components/servers/ServerPubsubImpl";

interface ServerRunning {
    name: string,
    status: string,
    pid: number
}

const OneClickButtons: any = memo((props: any) => {
    const intl = useIntl();

    return <Space size={'middle'} style={{margin: "-20px 0 10px 20px"}}>
        <Button type={'primary'}
                loading={props.oneClickLoading}
                onClick={props.oneClickRestart}>
            {intl.formatMessage({id: 'ONE_KEY_RESTART'})}
        </Button>
        <Button loading={props.oneClickLoading}
                onClick={props.oneClickStart}>
            {intl.formatMessage({id: 'ONE_KEY_START'})}
        </Button>
        <Button loading={props.oneClickLoading}
                onClick={props.oneClickStop}>
            {intl.formatMessage({id: 'ONE_KEY_STOP'})}
        </Button>
    </Space>;
});

const pubsub: PublishSubmit = new ServerPubsubImpl();

export default class ServerMgrView extends React.Component {
    state = {command: '', executing: false, loading: false, data: [], selectedRowKeys: [], selectRows: [], current: '', oneClickLoading: false};
    allServerOut: any = [];
    //methodMap = new Map();
    height = window.innerHeight - 120;
    componentDidMount() {
        this.refreshWebServerList();
        //初始化websocket的事件处理
        WsManager.addMessageHandler(MSG_EVENT.CONSOLE_LINE, this._console);
        WsManager.addMessageHandler(MSG_EVENT.SERVER_STATUS, this._serverStatusChange);
        WsManager.addMessageHandler(MSG_EVENT.CMD_COMPLETE, this._commandComplete);
    }

    componentWillUnmount() {
        WsManager.removeMessageHandler(MSG_EVENT.CONSOLE_LINE);
        WsManager.removeMessageHandler(MSG_EVENT.SERVER_STATUS);
        WsManager.removeMessageHandler(MSG_EVENT.CMD_COMPLETE);
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
                CommonNotice.error(`启动服务${server}失败！`);
                this._updateServerStatus(server, JarBootConst.STATUS_STOPPED);
                break;
            case JarBootConst.MSG_TYPE_STARTED:
                Logger.log(`启动成功${server}`);
                pubsub.publish(server, 'finishLoading');
                this._updateServerStatus(server, JarBootConst.STATUS_STARTED)
                break;
            case JarBootConst.MSG_TYPE_STOP_ERROR:
                Logger.log(`停止失败${server}`);
                CommonNotice.error(`停止服务${server}失败！`);
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

    private _commandComplete = (data: MsgData) => {
        this.setState({executing: false});
        pubsub.publish(data.server, 'finishLoading');
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
                    render: (text: string) => this._translateStatus(text),
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
    private _translateStatus(status: string) {
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
                if (!selectedRows || selectedRows.length <= 0 ||
                    !selectedRowKeys.includes(this.state.current)) {
                    current = '';
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
        let allList = [""];
        if (servers && servers.length > 0) {
            allList = allList.concat(servers.map(value => value.name));
        }
        this.allServerOut = allList;
    }

    private refreshWebServerList = () => {
        this.setState({loading: true});
        ServerMgrService.getServerList((resp: any) => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.error(resp.resultMsg);
                return;
            }
            this.setState({data: resp.result});
          this._initAllServerOut(resp.result);
        }, (errorMsg: any) => Logger.warn(`${ErrorUtil.formatErrResp(errorMsg)}`));
    };

    private _finishCallback = (resp: any) => {
        this.setState({loading: false});
        if (resp.resultCode < 0) {
            CommonNotice.error(resp.resultMsg);
        }
    };

    private startServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info('请选择一个服务后操作');
            return;
        }
        this.setState({out: "", loading: true});
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.startServer(this.state.selectedRowKeys, this._finishCallback,
                (errorMsg: any) => Logger.log(ErrorUtil.formatErrResp(errorMsg)));
    };

    private _clearDisplay = (server: string) => {
        pubsub.publish(server, 'clear');
    };

    private stopServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info('请选择一个服务后操作');
            return;
        }
        this.setState({out: "", loading: true});
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.stopServer(this.state.selectedRowKeys, this._finishCallback,
                (errorMsg: any) => CommonNotice.error(ErrorUtil.formatErrResp(errorMsg)));
    };
    private restartServer = () => {
        if (this.state.selectedRowKeys.length < 1) {
            CommonNotice.info('请选择一个服务后操作');
            return;
        }
        this.setState({out: "", loading: true});
        this.state.selectedRowKeys.forEach(this._clearDisplay);
        ServerMgrService.restartServer(this.state.selectedRowKeys, this._finishCallback,
                (errorMsg: any) => CommonNotice.error(ErrorUtil.formatErrResp(errorMsg)));
    };
    private _getTbBtnProps = () => {
        return [
            {
                name: '启动',
                key: 'start ',
                icon: <CaretRightFilled style={{color: 'green', fontSize: '18px'}}/>,
                onClick: this.startServer,
            },
            {
                name: '停止',
                key: 'stop',
                icon: <PoweroffOutlined style={{color: 'red', fontSize: '18px'}}/>,
                onClick: this.stopServer,
            },
            {
                name: '重启',
                key: 'reset',
                icon: <ReloadOutlined style={{color: '#1890ff', fontSize: '18px'}}/>,
                onClick: this.restartServer,
            },
            {
                name: '刷新',
                key: 'refresh',
                icon: <SyncOutlined style={{color: '#1890ff', fontSize: '18px'}}/>,
                onClick: this.refreshWebServerList,
            }
        ]
    };

    private oneClickRestart = () => {
        this._disableOnClickButton();
        ServerMgrService.oneClickRestart();
    };

    private oneClickStart = () => {
        this._disableOnClickButton();
        ServerMgrService.oneClickStart();
    };

    private oneClickStop = () => {
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

    private _getCurrentServer() {
        if (StringUtil.isEmpty(this.state.current)) {
            this.setState({current: ''});
            return '';
        }
        const keys: string[] = this.state.selectedRowKeys;
        if (!keys || keys.length <= 0) {
            this.setState({current: ''});
            return '';
        }
        if (keys.includes(this.state.current)) {
            return this.state.current;
        }
        this.setState({current: ''});
        return '';
    }

    private _onExecCommand = () => {
        if (StringUtil.isEmpty(this.state.command)) {
            return;
        }
        const server = this._getCurrentServer();
        if (StringUtil.isEmpty(server)) {
            CommonNotice.info('请选择一个服务后操作');
            return;
        }

        this.setState({executing: true});
        pubsub.publish(server, 'appendLine', `>${this.state.command}`);
        const msg = {server, body: this.state.command, func: 1};
        WsManager.sendMessage(JSON.stringify(msg));
    };

    private _onCancelCommand = () => {
        const server = this._getCurrentServer();
        if (StringUtil.isEmpty(server)) {
            CommonNotice.info('请选择一个服务后操作');
            return;
        }
        const msg = {server, body: '', func: 2};
        WsManager.sendMessage(JSON.stringify(msg));
    };

    render() {
        let tableOption: any = this._getTbProps();
        tableOption.scroll = { y: this.height};
        let outTitle = (<>
            <Input onPressEnter={this._onExecCommand}
                   disabled={this.state.executing}
                   placeholder={"command..."}
                   autoComplete={"off"}
                   autoCorrect="off"
                   autoCapitalize="off"
                   spellCheck="false"
                   style={{width: '100%'}}
                   onChange={event => this.setState({command: event.target.value})}
                   value={this.state.command}
                   suffix={this.state.executing && <LoadingOutlined/>}
                   addonAfter={this.state.executing ?
                       <CloseCircleOutlined onClick={this._onCancelCommand}/> :
                       <EnterOutlined onClick={this._onExecCommand}/>}/>
        </>);

        return (<div>
            <OneClickButtons loading={this.state.oneClickLoading}
                             oneClickRestart={this.oneClickRestart}
                             oneClickStart={this.oneClickStart}
                             oneClickStop={this.oneClickStop}/>
            <div style={{display: 'flex'}}>
                <div style={{flex: 'inherit', width: '28%'}}>
                    <CommonTable bordered tableOption={tableOption} tableButtons={this._getTbBtnProps()} height={this.height}/>
                </div>
                <div style={{flex: 'inherit', width: '72%'}}>
                    <Card title={outTitle} size={"small"}
                          extra={<Button type={"link"} onClick={() => this._clearDisplay(this.state.current)}>{formatMsg('CLEAR')}</Button>}>
                        <div className={styles.outPanel}>
                            <Console key={'-1'} visible={this.state.current === ""}
                                     server={''}
                                     pubsub={pubsub}/>
                            {this.allServerOut.map((value: any) => (
                                <Console key={value} visible={this.state.current === value}
                                         server={value}
                                         pubsub={pubsub}/>
                            ))}
                        </div>
                    </Card>
                </div>
            </div>
        </div>);
    }
}
