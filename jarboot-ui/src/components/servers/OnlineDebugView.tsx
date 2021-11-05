import CommonTable from "@/components/table";
import {Result, Tooltip} from "antd";
import {LoadingOutlined, BugOutlined, SyncOutlined, DashboardOutlined} from "@ant-design/icons";
import {formatMsg} from "@/common/IntlFormat";
import ServerMgrService, {JvmProcess} from "@/services/ServerMgrService";
import {SuperPanel} from "@/components/servers/SuperPanel";
import * as React from "react";
import {PUB_TOPIC, pubsub} from "@/components/servers/ServerPubsubImpl";
import CommonNotice, {notSelectInfo} from "@/common/CommonNotice";
import styles from "./index.less";
import {JarBootConst} from "@/common/JarBootConst";
import {MsgData} from "@/common/WsManager";

export default class OnlineDebugView extends React.PureComponent {
    state = {loading: false, data: [] as JvmProcess[],
        selectedRowKeys: [] as number[], searchText: '', searchedColumn: '', filteredInfo: null as any,
        selectRows: [] as JvmProcess[]};
    height = window.innerHeight - 120;
    componentDidMount() {
        this.refreshProcessList(true);
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, this.refreshProcessList);
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, this.onStatusChange);
    }

    componentWillUnmount() {
        pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, this.refreshProcessList);
        pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, this.onStatusChange);
    }

    private refreshProcessList = (init: boolean = false) => {
        this.setState({loading: true});
        ServerMgrService.getJvmProcesses((resp: any) => {
            this.setState({loading: false});
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            const data = resp.result as JvmProcess[];
            if (this.state.selectedRowKeys?.length) {
                const pid = this.state.selectedRowKeys[0];
                if (-1 === data.findIndex(item => pid === item.pid)) {
                    init = true;
                }
            }
            if (init) {
                //初始化选中第一个
                let selectedRowKeys = [] as number[];
                let selectRows: any = [];
                if (data.length > 0) {
                    const first: JvmProcess = data[0];
                    selectedRowKeys = [first.pid];
                    selectRows = [first];
                }
                this.setState({data, selectedRowKeys, selectRows});
                return;
            }
            this.setState({data});
        });
    };

    private onStatusChange = (data: MsgData) => {
        const process = this.state.data.find(item => data.sid === `${item.pid}`);
        if (!process) {
            return;
        }
        const status = data.body;
        switch (status) {
            case JarBootConst.MSG_TYPE_ONLINE:
                process.attached = true;
                break;
            case JarBootConst.MSG_TYPE_OFFLINE:
                process.attached = false;
                break;
            default:
                return;
        }
        this.setState({data: [...this.state.data]});
        pubsub.publish(data.sid, JarBootConst.FINISH_LOADING);
    };

    private _getTbProps() {
        return {
            columns: [
                {
                    title: 'PID',
                    dataIndex: 'pid',
                    key: 'pid',
                    width: 80,
                    ellipsis: true,
                    sorter: (a: JvmProcess, b: JvmProcess) => a.pid - b.pid,
                    sortDirections: ['descend', 'ascend'],
                    render: (value: number)=> <span style={{fontSize: '10px'}}>{value}</span>
                },
                {
                    title: formatMsg('NAME'),
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                    sorter: (a: JvmProcess, b: JvmProcess) => a.name.localeCompare(b.name),
                    sortDirections: ['descend', 'ascend'],
                },
            ],
            loading: this.state.loading,
            dataSource: this.state.data,
            pagination: false,
            rowKey: 'pid',
            size: 'small',
            rowSelection: this._getRowSelection(),
            onRow: this._onRow.bind(this),
            showHeader: true,
            scroll: this.height,
        };
    }

    private _getRowSelection() {
        return {
            columnWidth: '60px',
            columnTitle: '',
            ellipsis: true,
            type: 'radio',
            onChange: (selectedRowKeys: string[], selectedRows: JvmProcess[]) => {
                this.setState({selectedRowKeys: selectedRowKeys, selectRows: selectedRows,});
            },
            selectedRowKeys: this.state.selectedRowKeys,
            renderCell: this._renderRowSelection
        };
    }

    private _renderRowSelection = (row: any, record: JvmProcess) => {
        const style = {fontSize: '16px', color: record.attached ? 'green' : 'grey'};
        return <Tooltip title={record.attached ? 'Attached' : 'Not attached'}
                        color={record.attached ? '#87d068' : '#2db7f5'}>
            <BugOutlined style={style}/>
        </Tooltip>;
    }

    private _onRow(record: JvmProcess) {
        return {
            onClick: () => {
                this.setState({
                    selectedRowKeys: [record.pid],
                    selectRows: [record],
                });
            },
        };
    }

    private attach = () => {
        const process = this.state.selectRows[0];
        if (!process) {
            notSelectInfo();
        }
        const sid = process.pid + '';
        if (process.attached) {
            pubsub.publish(sid, JarBootConst.APPEND_LINE, "Already attached.");
            pubsub.publish(sid, JarBootConst.FINISH_LOADING);
        }
        pubsub.publish(sid, JarBootConst.APPEND_LINE, "Attaching...");
        ServerMgrService.attach(process.pid, process.name).then(resp => {
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
        }).catch(CommonNotice.errorFormatted);
    };

    private dashboardCmd = () => {
        if (this.state.selectRows?.length < 1) {
            notSelectInfo();
            return;
        }
        const process = this.state.selectRows[0];
        pubsub.publish(process.pid + '', PUB_TOPIC.QUICK_EXEC_CMD, "dashboard");
    };

    private _getTbBtnProps = () => {
        return [
            {
                name: 'Attach',
                key: 'attach ',
                icon: <BugOutlined className={styles.toolButtonGreenStyle}/>,
                onClick: this.attach,
                disabled: !this.state.selectRows?.length || this.state.selectRows[0].attached
            },
            {
                name: 'Refresh',
                key: 'refresh',
                icon: <SyncOutlined className={styles.toolButtonStyle}/>,
                onClick: () => this.refreshProcessList(),
            },
            {
                name: 'Dashboard',
                key: 'dashboard',
                icon: <DashboardOutlined className={styles.toolButtonRedStyle}/>,
                onClick: this.dashboardCmd,
                disabled: this.state.selectRows?.length && !this.state.selectRows[0].attached
            }
        ]
    };

    render() {
        let tableOption: any = this._getTbProps();
        tableOption.scroll = { y: this.height};
        const loading = this.state.loading && 0 === this.state.data.length;
        return <div style={{display: 'flex'}}>
            <div style={{flex: 'inherit', width: '28%'}}>
                <CommonTable toolbarGap={5} option={tableOption} toolbar={this._getTbBtnProps()}
                             showToolbarName={true}
                             height={this.height}/>
            </div>
            <div style={{flex: 'inherit', width: '72%'}}>
                {loading && <Result icon={<LoadingOutlined/>} title={formatMsg('LOADING')}/>}
                {this.state.data.map((value: JvmProcess) => (
                    <SuperPanel key={value.pid}
                                server={value.name}
                                sid={value.pid + ''}
                                visible={this.state.selectedRowKeys[0] === value.pid}/>
                ))}
            </div>
        </div>;
    }
};

