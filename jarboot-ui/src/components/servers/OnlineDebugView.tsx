import CommonTable from "@/components/table";
import {Result, Tooltip} from "antd";
import {LoadingOutlined, BugFilled, SyncOutlined, DashboardOutlined} from "@ant-design/icons";
import ServerMgrService, {JvmProcess} from "@/services/ServerMgrService";
import {SuperPanel} from "@/components/servers/SuperPanel";
import * as React from "react";
import {PUB_TOPIC, pubsub} from "@/components/servers/ServerPubsubImpl";
import CommonNotice, {notSelectInfo} from "@/common/CommonNotice";
import styles from "./index.less";
import { JarBootConst, MsgData } from "@/common/JarBootConst";
import {useEffect, useReducer} from "react";
import {useIntl} from "umi";
import {RemoteIcon} from "@/components/icons";

interface OnlineDebugState {
    loading: boolean;
    data: JvmProcess[];
    selectedRowKeys: string[];
    selectRows: JvmProcess[];
}

const OnlineDebugView = () => {
    const intl = useIntl();
    const initArg = {loading: true, data: [], selectedRowKeys: [], selectRows: []} as OnlineDebugState;
    const [state, dispatch] = useReducer((state: OnlineDebugState, action: any) => {
        if ('function' === typeof action) {
            return {...state, ...action(state)};
        }
        return {...state, ...action};
    }, initArg, arg => ({...arg}));

    const height = window.innerHeight - 120;
    useEffect(() => {
        refreshProcessList(true);
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, refreshProcessList);
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, onStatusChange);
        return () => {
            pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, refreshProcessList);
            pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, onStatusChange);
        }
    }, []);

    const refreshProcessList = (init: boolean = false, callback?: (data: JvmProcess[]) => any) => {
        dispatch({loading: true});
        ServerMgrService.getJvmProcesses((resp: any) => {
            if (resp.resultCode < 0) {
                dispatch({loading: false});
                CommonNotice.errorFormatted(resp);
                return;
            }
            const data = resp.result as JvmProcess[] || [];
            dispatch((s: OnlineDebugState) => {
                if (s.selectedRowKeys?.length) {
                    const sid = s.selectedRowKeys[0];
                    if (-1 === data.findIndex(item => sid === item.sid)) {
                        init = true;
                    }
                }
                let selectedRowKeys = s.selectedRowKeys || [] as string[];
                let selectRows = s.selectRows || [] as JvmProcess[];
                if (init) {
                    //初始化选中第一个
                    if (data && data?.length > 0) {
                        const first: JvmProcess = data[0];
                        selectedRowKeys = [first.sid];
                        selectRows = [first];
                    }
                }
                if (callback) {
                    return {loading: false, data, selectedRowKeys, selectRows, ...callback(data)};
                }
                return {loading: false, data, selectedRowKeys, selectRows};
            });
        });
    };

    const onStatusChange = (msg: MsgData) => {
        dispatch((s: OnlineDebugState) => {
            const data = s.data || [];
            const process = data?.find(item => msg.sid === item.sid);
            if (!process) {
                refreshProcessList(false, (data) => {
                    const item = data.find(value => msg.sid === value.sid);
                    if (!item) {
                        return {};
                    }
                    const selectedRowKeys = [msg.sid];
                    const selectRows = [item];
                    return {selectedRowKeys, selectRows};
                });
                return {};
            }
            const status = msg.body;
            switch (status) {
                case JarBootConst.ATTACHING:
                    process.attaching = true;
                    pubsub.publish(msg.sid, PUB_TOPIC.FOCUS_CMD_INPUT);
                    break;
                case JarBootConst.ATTACHED:
                    process.attached = true;
                    process.attaching = false;
                    pubsub.publish(msg.sid, PUB_TOPIC.FOCUS_CMD_INPUT);
                    break;
                case JarBootConst.EXITED:
                    process.attached = false;
                    process.attaching = false;
                    refreshProcessList();
                    break;
                default:
                    return {};
            }
            const selectedRowKeys = [process.sid];
            const selectRows = [process];
            return {data, selectRows, selectedRowKeys};
        });
        pubsub.publish(msg.sid, JarBootConst.FINISH_LOADING);
    };

    const getTbProps = () => ({
        columns: [
            {
                title: 'PID',
                dataIndex: 'pid',
                key: 'pid',
                width: 80,
                ellipsis: true,
                sorter: (a: JvmProcess, b: JvmProcess) => a.pid - b.pid,
                sortDirections: ['descend', 'ascend'],
                render: (value: number) => <span style={{fontSize: '10px'}}>{value}</span>
            },
            {
                title: intl.formatMessage({id: 'NAME'}),
                dataIndex: 'name',
                key: 'name',
                ellipsis: true,
                sorter: (a: JvmProcess, b: JvmProcess) => a.name.localeCompare(b.name),
                sortDirections: ['descend', 'ascend'],
            },
        ],
        loading: state.loading,
        dataSource: state.data,
        pagination: false,
        rowKey: 'sid',
        size: 'small',
        rowSelection: getRowSelection(),
        onRow: onRow,
        showHeader: true,
        scroll: height,
    });

    const getRowSelection = () => ({
        columnWidth: '60px',
        columnTitle: '',
        ellipsis: true,
        type: 'radio',
        onChange: (selectedRowKeys: number[], selectRows: JvmProcess[]) =>
            dispatch({selectedRowKeys, selectRows}),
        selectedRowKeys: state.selectedRowKeys,
        renderCell: renderRowSelection
    });

    const renderRowSelection = (row: any, record: JvmProcess) => {
        if (record.attaching) {
            return (<Tooltip title={'Attaching'}>
                <LoadingOutlined className={styles.statusStarting}/>
            </Tooltip>);
        }
        if (record.remote) {
            //远程vm
            return (<Tooltip title={record.remote}>
                <RemoteIcon className={styles.remoteOnlineStatus}/>
            </Tooltip>);
        }
        const className = record.attached ? styles.attachedStatus : styles.noAttachedStatus;
        return (<Tooltip title={record.attached ? 'Attached' : 'Not attached'}>
            <BugFilled className={className}/>
        </Tooltip>);
    };

    const onRow = (record: JvmProcess) => ({
        onClick: () => {
            dispatch({selectedRowKeys: [record.sid], selectRows: [record]});
            pubsub.publish(record.sid, PUB_TOPIC.FOCUS_CMD_INPUT);
        },
    });

    const attach = () => {
        const process = state?.selectRows[0];
        if (!process) {
            notSelectInfo();
        }
        const sid = process.sid;
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

    const dashboardCmd = () => {
        if (state.selectRows?.length < 1) {
            notSelectInfo();
            return;
        }
        const process = state.selectRows[0];
        pubsub.publish(process.sid, PUB_TOPIC.QUICK_EXEC_CMD, "dashboard");
    };

    const getTbBtnProps = () => ([
        {
            title: 'Attach',
            key: 'attach ',
            icon: <BugFilled className={styles.toolButtonIcon}/>,
            onClick: attach,
            disabled: !state.selectRows?.length || state.selectRows[0].attached
        },
        {
            title: intl.formatMessage({id: 'REFRESH_BTN'}),
            key: 'refresh',
            icon: <SyncOutlined className={styles.toolButtonIcon}/>,
            onClick: () => refreshProcessList(),
        },
        {
            title: intl.formatMessage({id: 'DASHBOARD'}),
            key: 'dashboard',
            icon: <DashboardOutlined className={styles.toolButtonRedIcon}/>,
            onClick: dashboardCmd,
            disabled: state.selectRows?.length && !state.selectRows[0].attached
        }
    ]);

    let tableOption: any = getTbProps();
    tableOption.scroll = {y: height};
    const showLoading = state.loading && 0 === state.data.length;
    return <div className={styles.serverMgr}>
        <div className={styles.serverMgrSide}>
            <CommonTable option={tableOption} toolbar={getTbBtnProps()}
                         showToolbarName={true}
                         height={height}/>
        </div>
        <div className={styles.serverMgrContent}>
            {showLoading && <Result icon={<LoadingOutlined/>} title={intl.formatMessage({id: 'LOADING'})}/>}
            {state.data.map((value: JvmProcess) => (
                <SuperPanel key={value.sid}
                            server={value.name}
                            sid={value.sid}
                            visible={state.selectedRowKeys[0] === value.sid}/>
            ))}
        </div>
    </div>;
};

export default OnlineDebugView;
