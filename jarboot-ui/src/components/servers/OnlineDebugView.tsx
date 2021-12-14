import CommonTable from "@/components/table";
import {Result, Tooltip} from "antd";
import {BugFilled, DashboardOutlined, HomeFilled, LoadingOutlined, SyncOutlined, CloseCircleFilled} from "@ant-design/icons";
import ServerMgrService, {JvmProcess, TreeNode} from "@/services/ServerMgrService";
import {SuperPanel} from "@/components/servers/SuperPanel";
import * as React from "react";
import {useEffect, useReducer} from "react";
import {PUB_TOPIC, pubsub} from "@/components/servers/ServerPubsubImpl";
import CommonNotice, {notSelectInfo} from "@/common/CommonNotice";
import styles from "./index.less";
import {JarBootConst, MsgData} from "@/common/JarBootConst";
import {useIntl} from "umi";
import {RemoteIcon} from "@/components/icons";
import IntlText from "@/common/IntlText";

interface OnlineDebugState {
    loading: boolean;
    data: JvmProcess[];
    list: JvmProcess[];
    selectedRowKeys: string[];
    selectRows: JvmProcess[];
    expandedRowKeys: string[];
}

const OnlineDebugView = () => {
    const intl = useIntl();
    const initArg = {
        loading: true,
        data: [],
        list: [],
        selectedRowKeys: [],
        selectRows: [],
        expandedRowKeys: []
    } as OnlineDebugState;
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
            const list = resp.result as JvmProcess[] || [];
            dispatch((s: OnlineDebugState) => {
                if (s.selectedRowKeys?.length) {
                    const sid = s.selectedRowKeys[0];
                    if (-1 === list.findIndex(item => sid === item.sid)) {
                        init = true;
                    }
                }
                let selectedRowKeys = s.selectedRowKeys || [] as string[];
                let selectRows = s.selectRows || [] as JvmProcess[];
                if (init) {
                    //初始化选中第一个
                    if (list && list?.length > 0) {
                        const first: JvmProcess = list[0];
                        selectedRowKeys = [first.sid];
                        selectRows = [first];
                    }
                }
                const data = getTreeData(list) as JvmProcess[];
                const expandedRowKeys = data.map(value => value.key);

                if (callback) {
                    return {loading: false, data, expandedRowKeys, list, selectedRowKeys, selectRows, ...callback(list)};
                }
                return {loading: false, data, expandedRowKeys, list, selectedRowKeys, selectRows};
            });
        });
    };

    const groupIcon = (vm: JvmProcess): React.ReactNode => {
        return <Tooltip title={vm.title}>
            {
                JarBootConst.LOCALHOST === vm.key ?
                    <HomeFilled className={styles.vmTreeGroupIcon}/>
                    :
                    <RemoteIcon className={styles.vmTreeGroupIcon}/>
            }
        </Tooltip>;
    };

    const childIcon = (vm: JvmProcess): React.ReactNode => {
        if (vm.attaching) {
            return (<Tooltip title={'Attaching'}>
                <LoadingOutlined className={styles.statusStarting}/>
            </Tooltip>);
        }
        return <Tooltip title={vm.attached ? "Attached" : "Not attached"}>
            <BugFilled className={vm.attached ? styles.attachedStatus : styles.noAttachedStatus}/>
        </Tooltip>;
    };

    const getTreeData = (data: JvmProcess[]): TreeNode[] => {
        const treeData = [] as TreeNode[];
        const groupMap = new Map<string, JvmProcess[]>();
        data.forEach(vm => {
            const group = vm.remote || JarBootConst.LOCALHOST;
            let children = groupMap.get(group);
            if (!children) {
                children = [] as JvmProcess[];
                groupMap.set(group, children);
                treeData.push({
                    title: group,
                    sid: group,
                    key: group,
                    name: group,
                    selectable: false,
                    children
                });
            }
            const child = vm as JvmProcess;
            child.key = vm.sid;
            child.title = vm.name;
            child.isLeaf = true;
            children.push(child);
        });
        return treeData;
    };

    const onStatusChange = (msg: MsgData) => {
        dispatch((s: OnlineDebugState) => {
            const list = s.list || [];
            const process = list?.find(item => msg.sid === item.sid);
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
            return {data: getTreeData(list), selectRows, selectedRowKeys};
        });
        pubsub.publish(msg.sid, JarBootConst.FINISH_LOADING);
    };

    const pidRender = (value: number, row: JvmProcess) => {
        if (row.isLeaf) {
            return value;
        }
        const id = JarBootConst.LOCALHOST === row.key ? 'LOCAL' : 'REMOTE';
        const text = <span className={styles.groupRow}>
            <IntlText id={id}/>
        </span>;
        return <Tooltip title={row.name}>{text}</Tooltip>;
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
                render: pidRender
            },
            {
                title: <IntlText id={"NAME"}/>,
                dataIndex: 'name',
                key: 'name',
                ellipsis: true,
                sorter: (a: JvmProcess, b: JvmProcess) => a.name.localeCompare(b.name),
                sortDirections: ['descend', 'ascend'],
                render: (value: string, row: TreeNode) =>
                    row.isLeaf ? value : <span className={styles.groupRow}>{value}</span>
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
        expandable: {
            defaultExpandAllRows: true,
            indentSize: 5,
            expandIconColumnIndex: 0,
            expandedRowKeys: state.expandedRowKeys,
            onExpandedRowsChange: (expandedRowKeys: string[]) => dispatch({expandedRowKeys}),
        }
    });

    const getRowSelection = () => ({
        columnWidth: 60,
        columnTitle: '',
        ellipsis: true,
        type: 'radio',
        fixed: true,
        onChange: (selectedRowKeys: number[], selectRows: JvmProcess[]) =>
            dispatch({selectedRowKeys, selectRows}),
        selectedRowKeys: state.selectedRowKeys,
        renderCell: renderRowSelection
    });

    const renderRowSelection = (row: any, record: JvmProcess) => {
        if (record.isLeaf) {
            return childIcon(record);
        }
        return groupIcon(record);
    };

    const onRow = (record: JvmProcess) => ({
        onClick: () => {
            if (record.isLeaf) {
                dispatch({selectedRowKeys: [record.sid], selectRows: [record]});
                pubsub.publish(record.sid, PUB_TOPIC.FOCUS_CMD_INPUT);
            }
        },
        onDoubleClick: () => {
            if (record.isLeaf && !record.attached) {
                attach();
            }
        }
    });

    const detach = () => {
        if (state.selectRows?.length < 1) {
            notSelectInfo();
            return;
        }
        const process = state.selectRows[0];
        pubsub.publish(process.sid, PUB_TOPIC.QUICK_EXEC_CMD, "shutdown");
    };

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
        ServerMgrService.attach(process.pid).then(resp => {
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

    const isAttached = (!state.selectRows?.length || state.selectRows[0].attached);

    const getTbBtnProps = () => ([
        {
            title: isAttached? 'Detach' : 'Attach',
            key: 'attach-detach',
            icon: isAttached ? <CloseCircleFilled className={styles.toolButtonIcon}/> : <BugFilled className={styles.toolButtonIcon}/>,
            onClick: isAttached ? detach : attach,
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
    const showLoading = state.loading && 0 === state.list.length;
    return <div className={styles.serverMgr}>
        <div className={styles.serverMgrSide}>
            <CommonTable option={tableOption} toolbar={getTbBtnProps()}
                         showToolbarName={true}
                         height={height}/>
        </div>
        <div className={styles.serverMgrContent}>
            {showLoading && <Result icon={<LoadingOutlined/>} title={intl.formatMessage({id: 'LOADING'})}/>}
            {state.list.map((value: JvmProcess) => (
                <SuperPanel key={value.sid}
                            server={value.name}
                            sid={value.sid}
                            visible={state.selectedRowKeys[0] === value.sid}/>
            ))}
        </div>
    </div>;
};

export default OnlineDebugView;
