import * as React from "react";
import {Result, Tag, Input, Space, Button} from "antd";
import { getLocale } from 'umi';
import ServerMgrService, {ServerRunning} from "@/services/ServerMgrService";
import CommonNotice from '@/common/CommonNotice';
import {
    SyncOutlined, CaretRightOutlined, ExclamationCircleOutlined, CaretRightFilled, DashboardOutlined,
    PoweroffOutlined, ReloadOutlined, UploadOutlined, LoadingOutlined, SearchOutlined
} from '@ant-design/icons';
import {JarBootConst} from '@/common/JarBootConst';
import {MsgData, WsManager} from "@/common/WsManager";
import Logger from "@/common/Logger";
import {MSG_EVENT} from "@/common/EventConst";
import {formatMsg} from "@/common/IntlFormat";
import {PUB_TOPIC, SuperPanel, pubsub} from "@/components/servers/SuperPanel";
import OneClickButtons from "@/components/servers/OneClickButtons";
import CommonTable from "@/components/table";
import UploadFileModal from "@/components/servers/UploadFileModal";
import StringUtil from "@/common/StringUtil";
// @ts-ignore
import Highlighter from 'react-highlight-words';

const toolButtonStyle = {color: '#1890ff', fontSize: '18px'};
const toolButtonRedStyle = {color: 'red', fontSize: '18px'};
const toolButtonGreenStyle = {color: 'green', fontSize: '18px'};

const notSelectInfo = () => {
    if (JarBootConst.ZH_CN === getLocale()) {
        CommonNotice.info('请点击选择一个服务执行');
    } else {
        CommonNotice.info('Please select one to operate');
    }
};

export default class ServerMgrView extends React.PureComponent {
    state = {loading: false, data: [] as ServerRunning[], uploadVisible: false,
        selectedRowKeys: [] as string[], searchText: '', searchedColumn: '', filteredInfo: null as any,
        selectRows: [] as ServerRunning[], current: '', oneClickLoading: false};
    height = window.innerHeight - 120;
    searchInput = {} as any;
    componentDidMount() {
        this.refreshServerList(true);
        pubsub.submit('', PUB_TOPIC.RECONNECTED, this.refreshServerList);
        //初始化websocket的事件处理
        WsManager.addMessageHandler(MSG_EVENT.SERVER_STATUS, this._serverStatusChange);
    }

    componentWillUnmount() {
        pubsub.unSubmit('', PUB_TOPIC.RECONNECTED, this.refreshServerList);
        WsManager.removeMessageHandler(MSG_EVENT.SERVER_STATUS);
    }

    private _activeConsole(sid: string) {
        let data: ServerRunning[] = this.state.data;
        const index = data.findIndex(row => row.sid === sid);
        if (-1 !== index) {
            const selectedRowKeys: any = [data[index].sid];
            const selectRows: any = [data[index]];
            this.setState({selectedRowKeys, selectRows});
        }
    }

    private _serverStatusChange = (data: MsgData) => {
        const status = data.body;
        const key = data.sid;
        const s = this.state.data.find(value => value.sid === data.sid);
        const server = s?.name as string;
        switch (status) {
            case JarBootConst.MSG_TYPE_START:
                // 激活终端显示
                this._activeConsole(key);
                Logger.log(`${server}启动中...`);
                pubsub.publish(key, JarBootConst.START_LOADING);
                this._clearDisplay({name: server, sid: key, path: '', status: ''});
                this._updateServerStatus(key, JarBootConst.STATUS_STARTING);
                break;
            case JarBootConst.MSG_TYPE_STOP:
                Logger.log(`${server}停止中...`);
                pubsub.publish(key, JarBootConst.START_LOADING);
                this._updateServerStatus(key, JarBootConst.STATUS_STOPPING);
                break;
            case JarBootConst.MSG_TYPE_START_ERROR:
                Logger.log(`${server}启动失败`);
                CommonNotice.error(`Start ${server} failed!`);
                this._updateServerStatus(key, JarBootConst.STATUS_STOPPED);
                break;
            case JarBootConst.MSG_TYPE_STARTED:
                Logger.log(`${server}启动成功`);
                pubsub.publish(key, JarBootConst.FINISH_LOADING);
                this._updateServerStatus(key, JarBootConst.STATUS_STARTED)
                break;
            case JarBootConst.MSG_TYPE_STOP_ERROR:
                Logger.log(`${server}停止失败`);
                CommonNotice.error(`Stop ${server} failed!`);
                this._updateServerStatus(key, JarBootConst.STATUS_STARTED);
                break;
            case JarBootConst.MSG_TYPE_STOPPED:
                Logger.log(`${server}停止成功`);
                pubsub.publish(key, JarBootConst.FINISH_LOADING);
                this._updateServerStatus(key, JarBootConst.STATUS_STOPPED)
                break;
            case JarBootConst.MSG_TYPE_RESTART:
                Logger.log(`${server}重启成功`);
                pubsub.publish(key, JarBootConst.FINISH_LOADING);
                this._updateServerStatus(key, JarBootConst.STATUS_STARTED)
                break;
            default:
                break;
        }
    };

    private _updateServerStatus(sid: string, status: string) {
        let data: ServerRunning[] = this.state.data;
        data = data.map((value: ServerRunning) => {
            if (value.sid === sid) {
                value.status = status;
            }
            return value;
        });
        this.setState({data});
    }

    getColumnSearchProps = (dataIndex: string) => ({
        // @ts-ignore
        filterDropdown: ({ setSelectedKeys, selectedKeys, confirm, clearFilters }) => (
            <div style={{ padding: 8 }}>
                <Input
                    ref={node => {
                        this.searchInput = node;
                    }}
                    placeholder={`Search ${dataIndex}`}
                    value={selectedKeys[0]}
                    onChange={e => setSelectedKeys(e.target.value ? [e.target.value] : [])}
                    onPressEnter={() => this.handleSearch(selectedKeys, confirm, dataIndex)}
                    style={{ marginBottom: 8, display: 'block' }}
                />
                <Space>
                    <Button
                        type="primary"
                        onClick={() => this.handleSearch(selectedKeys, confirm, dataIndex)}
                        icon={<SearchOutlined />}
                        size="small"
                        style={{ width: 90 }}
                    >
                        Search
                    </Button>
                    <Button onClick={() => this.handleReset(clearFilters)} size="small" style={{ width: 90 }}>
                        Reset
                    </Button>
                    <Button
                        type="link"
                        size="small"
                        onClick={() => {
                            confirm({ closeDropdown: false });
                            this.setState({
                                searchText: selectedKeys[0],
                                searchedColumn: dataIndex,
                            });
                        }}
                    >
                        Filter
                    </Button>
                </Space>
            </div>
        ),
        filterIcon: (filtered: boolean) => <SearchOutlined style={{ color: filtered ? '#1890ff' : undefined }} />,
        onFilter: (value: string, record: any) =>
            record[dataIndex]
                ? record[dataIndex].toString().toLowerCase().includes(value.toLowerCase())
                : '',
        onFilterDropdownVisibleChange: (visible: boolean) => {
            if (visible) {
                setTimeout(() => this.searchInput.select(), 100);
            }
        },
        render: (text: string) =>
            this.state.searchedColumn === dataIndex ? (
                <Highlighter
                    highlightStyle={{ backgroundColor: '#ffc069', padding: 0 }}
                    searchWords={[this.state.searchText]}
                    autoEscape
                    textToHighlight={text ? text.toString() : ''}
                />
            ) : (
                text
            ),
    });

    handleSearch = (selectedKeys: string[], confirm: () => void, dataIndex: string) => {
        confirm();
        this.setState({
            searchText: selectedKeys[0],
            searchedColumn: dataIndex,
        });
    };

    handleReset = (clearFilters: () => void) => {
        clearFilters();
        this.setState({ searchText: '' });
    };

    handleChange = (pagination: any, filters: any, sorter: any) => {
        console.log('Various parameters', pagination, filters, sorter);
        this.setState({filteredInfo: filters});
    };

    clearFilters = () => {
        this.setState({ filteredInfo: null });
    };

    clearAll = () => {
        this.setState({
            filteredInfo: null,
            sortedInfo: null,
        });
    };

    private _getTbProps() {
        let { filteredInfo } = this.state;
        filteredInfo = filteredInfo || {};
        return {
            columns: [
                {
                    title: formatMsg('NAME'),
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                    sorter: (a: ServerRunning, b: ServerRunning) => a.name.localeCompare(b.name),
                    sortDirections: ['descend', 'ascend'],
                    ...this.getColumnSearchProps('name')
                },
                {
                    title: formatMsg('STATUS'),
                    dataIndex: 'status',
                    key: 'status',
                    ellipsis: true,
                    width: 120,
                    filters: [
                        { text: 'STOPPED', value: 'STOPPED' },
                        { text: 'RUNNING', value: 'RUNNING' },
                        { text: 'STARTING', value: 'STARTING' },
                        { text: 'STOPPING', value: 'STOPPING' },
                    ],
                    filteredValue: filteredInfo.status || null,
                    onFilter: (value: string, record: ServerRunning) => record.status.includes(value),
                    render: (text: string) => ServerMgrView.translateStatus(text),
                },
            ],
            loading: this.state.loading,
            dataSource: this.state.data,
            pagination: false,
            rowKey: 'sid',
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
            onChange: (selectedRowKeys: string[], selectedRows: ServerRunning[]) => {
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

    private _onRow(record: ServerRunning) {
        return {
            onClick: () => {
                this.setState({
                    selectedRowKeys: [record.sid],
                    selectRows: [record],
                    current: record.sid,
                });
            },
        };
    }

    private refreshServerList = (init: boolean = false) => {
        this.setState({loading: true});
        ServerMgrService.getServerList((resp: any) => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            const data = resp.result as ServerRunning[];
            if (init) {
                //初始化选中第一个
                let current = '';
                let selectedRowKeys = [] as string[];
                let selectRows: any = [];
                if (data.length > 0) {
                    const first: ServerRunning = data[0];
                    current = first.sid ;
                    selectedRowKeys = [first.sid];
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
        if (this.state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }
        this.setState({loading: true});
        this.state.selectRows.forEach(this._clearDisplay);
        ServerMgrService.startServer(this.state.selectRows, this._finishCallback);
    };

    private _clearDisplay = (server: ServerRunning) => {
        const key = server.sid;
        pubsub.publish(key, JarBootConst.CLEAR_CONSOLE);
    };

    private stopServer = () => {
        if (this.state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }

        this.setState({loading: true});

        ServerMgrService.stopServer(this.state.selectRows, this._finishCallback);
    };

    private restartServer = () => {
        if (this.state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }
        this.setState({loading: true});
        this.state.selectRows.forEach(this._clearDisplay);
        ServerMgrService.restartServer(this.state.selectRows, this._finishCallback);
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
        pubsub.publish(this.state.current, JarBootConst.APPEND_LINE, "Restarting all...");
        this._disableOnClickButton();
        ServerMgrService.oneClickRestart();
    };

    private oneClickStart = () => {
        pubsub.publish(this.state.current, JarBootConst.APPEND_LINE, "Starting all...");
        this._disableOnClickButton();
        ServerMgrService.oneClickStart();
    };

    private oneClickStop = () => {
        pubsub.publish(this.state.current, JarBootConst.APPEND_LINE, "Stopping all...");
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
        const loading = this.state.loading && 0 === this.state.data.length;
        return (<div>
            <div style={{display: 'flex'}}>
                <div style={{flex: 'inherit', width: '28%'}}>
                    <CommonTable toolbarGap={5} option={tableOption}
                                 toolbar={this._getTbBtnProps()} height={this.height}/>
                    <OneClickButtons loading={this.state.oneClickLoading}
                                     oneClickRestart={this.oneClickRestart}
                                     oneClickStart={this.oneClickStart}
                                     oneClickStop={this.oneClickStop}/>
                </div>
                <div style={{flex: 'inherit', width: '72%'}}>
                    {loading && <Result icon={<LoadingOutlined/>} title={formatMsg('LOADING')}/>}
                    {this.state.data.map((value: ServerRunning) => (
                        <SuperPanel key={value.sid}
                                    server={value.name}
                                    sid={value.sid}
                                    visible={this.state.current === value.sid}/>
                    ))}
                </div>
            </div>
            {this.state.uploadVisible && <UploadFileModal server={this.state.selectRows[0].name}
                             onClose={this.onUploadClose}/>}
        </div>);
    }
}
