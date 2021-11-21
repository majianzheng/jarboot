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

interface OnlineDebugState {
    loading: boolean;
    data: JvmProcess[];
    selectedRowKeys: number[];
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
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
        return () => {
            pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, refreshProcessList);
            pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
        }
    }, []);

    const refreshProcessList = (init: boolean = false) => {
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
                    const pid = s.selectedRowKeys[0];
                    if (-1 === data.findIndex(item => pid === item.pid)) {
                        init = true;
                    }
                }
                if (init) {
                    //初始化选中第一个
                    let selectedRowKeys = [] as number[];
                    let selectRows = [] as JvmProcess[];
                    if (data && data?.length > 0) {
                        const first: JvmProcess = data[0];
                        selectedRowKeys = [first.pid];
                        selectRows = [first];
                    }
                    return {loading: false, data, selectedRowKeys, selectRows};
                }
                return {loading: false, data};
            });
        });
    };

    const onStatusChange = (msg: MsgData) => {
        dispatch((s: OnlineDebugState) => {
            const data = s.data || [];
            const process = data?.find(item => msg.sid === `${item.pid}`);
            if (!process) {
                return {};
            }
            const status = msg.body;
            switch (status) {
                case JarBootConst.STATUS_STARTING:
                case JarBootConst.STATUS_STARTED:
                    process.attached = true;
                    pubsub.publish(msg.sid, PUB_TOPIC.FOCUS_CMD_INPUT);
                    break;
                case JarBootConst.STATUS_STOPPING:
                case JarBootConst.STATUS_STOPPED:
                    process.attached = false;
                    refreshProcessList();
                    break;
                default:
                    return {};
            }
            return {data};
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
        rowKey: 'pid',
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
        const style = {fontSize: '16px', color: record.attached ? 'green' : 'grey'};
        return <Tooltip title={record.attached ? 'Attached' : 'Not attached'}
                        color={record.attached ? '#87d068' : '#2db7f5'}>
            <BugFilled style={style}/>
        </Tooltip>;
    };

    const onRow = (record: JvmProcess) => ({
        onClick: () => {
            dispatch({selectedRowKeys: [record.pid], selectRows: [record]});
            pubsub.publish(`${record.pid}`, PUB_TOPIC.FOCUS_CMD_INPUT);
        },
    });

    const attach = () => {
        const process = state?.selectRows[0];
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

    const dashboardCmd = () => {
        if (state.selectRows?.length < 1) {
            notSelectInfo();
            return;
        }
        const process = state.selectRows[0];
        pubsub.publish(process.pid + '', PUB_TOPIC.QUICK_EXEC_CMD, "dashboard");
    };

    const getTbBtnProps = () => ([
        {
            name: 'Attach',
            key: 'attach ',
            icon: <BugFilled className={styles.toolButtonGreenStyle}/>,
            onClick: attach,
            disabled: !state.selectRows?.length || state.selectRows[0].attached
        },
        {
            name: intl.formatMessage({id: 'REFRESH_BTN'}),
            key: 'refresh',
            icon: <SyncOutlined className={styles.toolButtonStyle}/>,
            onClick: () => refreshProcessList(),
        },
        {
            name: intl.formatMessage({id: 'DASHBOARD'}),
            key: 'dashboard',
            icon: <DashboardOutlined className={styles.toolButtonRedStyle}/>,
            onClick: dashboardCmd,
            disabled: state.selectRows?.length && !state.selectRows[0].attached
        }
    ]);

    let tableOption: any = getTbProps();
    tableOption.scroll = {y: height};
    const showLoading = state.loading && 0 === state.data.length;
    return <div style={{display: 'flex'}}>
        <div style={{flex: 'inherit', width: '28%'}}>
            <CommonTable toolbarGap={5} option={tableOption} toolbar={getTbBtnProps()}
                         showToolbarName={true}
                         height={height}/>
        </div>
        <div style={{flex: 'inherit', width: '72%'}}>
            {showLoading && <Result icon={<LoadingOutlined/>} title={intl.formatMessage({id: 'LOADING'})}/>}
            {state.data.map((value: JvmProcess) => (
                <SuperPanel key={value.pid}
                            server={value.name}
                            sid={value.pid + ''}
                            visible={state.selectedRowKeys[0] === value.pid}/>
            ))}
        </div>
    </div>;
};

export default OnlineDebugView;
