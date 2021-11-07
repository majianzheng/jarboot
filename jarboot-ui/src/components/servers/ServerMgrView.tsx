import * as React from "react";
import {Result, Tag, Input, Space, Button, Modal} from "antd";
import ServerMgrService, {ServerRunning} from "@/services/ServerMgrService";
import CommonNotice, {notSelectInfo} from '@/common/CommonNotice';
import {
    SyncOutlined, CaretRightOutlined, CaretRightFilled, DashboardOutlined, DeleteOutlined,
    PoweroffOutlined, ReloadOutlined, UploadOutlined, LoadingOutlined, SearchOutlined
} from '@ant-design/icons';
import {JarBootConst} from '@/common/JarBootConst';
import {MsgData} from "@/common/WsManager";
import Logger from "@/common/Logger";
import {PUB_TOPIC, SuperPanel, pubsub} from "@/components/servers";
import OneClickButtons from "@/components/servers/OneClickButtons";
import CommonTable from "@/components/table";
import UploadFileModal from "@/components/servers/UploadFileModal";
import StringUtil from "@/common/StringUtil";
// @ts-ignore
import Highlighter from 'react-highlight-words';
import styles from "./index.less";
import {useEffect, useReducer} from "react";
import {useIntl} from "umi";

let searchInput = {} as any;

const ServerMgrView = () => {
    const intl = useIntl();
    const initArg = {
        loading: false, data: [] as ServerRunning[], uploadVisible: false,
        selectedRowKeys: [] as string[], searchText: '', searchedColumn: '',
        selectRows: [] as ServerRunning[], current: '', oneClickLoading: false
    };
    const [state, dispatch] = useReducer((state: any, action: any) => {
        if ('function' === typeof action) {
            return {...state, ...action(state)};
        }
        return {...state, ...action};
    }, initArg, arg => ({...arg}));

    const height = window.innerHeight - 120;

    useEffect(() => {
        refreshServerList(true);
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, refreshServerList);
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, refreshServerList);
        pubsub.submit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
        return () => {
            pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, refreshServerList);
            pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, refreshServerList);
            pubsub.unSubmit(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, onStatusChange);
        }
    }, []);

    const activeConsole = (sid: string) => {
        dispatch((preState: any) => {
            let data: ServerRunning[] = preState.data;
            const index = data.findIndex(row => row.sid === sid);
            if (-1 !== index) {
                const selectedRowKeys: any = [data[index].sid];
                const selectRows: any = [data[index]];
                return {selectedRowKeys, selectRows};
            }
            return {};
        });
    };

    const onStatusChange = (data: MsgData) => {
        dispatch((preState: any) => {
            const item = preState.data.find((value: ServerRunning) => value.sid === data.sid);
            if (!item) {
                return {};
            }
            const status = data.body;
            const key = data.sid;
            const server = item.name as string;
            switch (status) {
                case JarBootConst.MSG_TYPE_START:
                    // 激活终端显示
                    activeConsole(key);
                    Logger.log(`${server}启动中...`);
                    pubsub.publish(key, JarBootConst.START_LOADING);
                    clearDisplay(item);
                    item.status = JarBootConst.STATUS_STARTING;
                    break;
                case JarBootConst.MSG_TYPE_STOP:
                    Logger.log(`${server}停止中...`);
                    pubsub.publish(key, JarBootConst.START_LOADING);
                    item.status = JarBootConst.STATUS_STOPPING;
                    break;
                case JarBootConst.MSG_TYPE_START_ERROR:
                    Logger.log(`${server}启动失败`);
                    CommonNotice.error(`Start ${server} failed!`);
                    item.status = JarBootConst.STATUS_STOPPED;
                    break;
                case JarBootConst.MSG_TYPE_STARTED:
                    Logger.log(`${server}启动成功`);
                    pubsub.publish(key, JarBootConst.FINISH_LOADING);
                    item.status = JarBootConst.STATUS_STARTED;
                    break;
                case JarBootConst.MSG_TYPE_STOP_ERROR:
                    Logger.log(`${server}停止失败`);
                    CommonNotice.error(`Stop ${server} failed!`);
                    item.status = JarBootConst.STATUS_STARTED;
                    break;
                case JarBootConst.MSG_TYPE_STOPPED:
                    Logger.log(`${server}停止成功`);
                    pubsub.publish(key, JarBootConst.FINISH_LOADING);
                    item.status = JarBootConst.STATUS_STOPPED;
                    break;
                case JarBootConst.MSG_TYPE_RESTART:
                    Logger.log(`${server}重启成功`);
                    pubsub.publish(key, JarBootConst.FINISH_LOADING);
                    item.status = JarBootConst.STATUS_STARTED;
                    break;
                default:
                    return {};
            }
            return {data: [...preState.data]};
        });

    };

    const getColumnSearchProps = (dataIndex: string) => ({
        // @ts-ignore
        filterDropdown: ({setSelectedKeys, selectedKeys, confirm, clearFilters}) => (
            <div style={{padding: 8}}>
                <Input
                    ref={node => {
                        searchInput = node;
                    }}
                    placeholder={`Search ${dataIndex}`}
                    value={selectedKeys[0]}
                    onChange={e => setSelectedKeys(e.target.value ? [e.target.value] : [])}
                    onPressEnter={() => handleSearch(selectedKeys, confirm, dataIndex)}
                    style={{marginBottom: 8, display: 'block'}}
                />
                <Space>
                    <Button
                        type="primary"
                        onClick={() => handleSearch(selectedKeys, confirm, dataIndex)}
                        icon={<SearchOutlined/>}
                        size="small"
                        style={{width: 90}}
                    >
                        {intl.formatMessage({id: 'SEARCH_BTN'})}
                    </Button>
                    <Button onClick={() => handleReset(clearFilters)} size="small" style={{width: 90}}>
                        {intl.formatMessage({id: 'RESET_BTN'})}
                    </Button>
                    <Button
                        type="link"
                        size="small"
                        onClick={() => {
                            confirm({closeDropdown: false});
                            dispatch({searchText: selectedKeys[0], searchedColumn: dataIndex});
                            state.searchText = selectedKeys[0];
                            state.searchedColumn = dataIndex;
                        }}
                    >
                        {intl.formatMessage({id: 'FILTER_BTN'})}
                    </Button>
                </Space>
            </div>
        ),
        filterIcon: (filtered: boolean) => <SearchOutlined style={{color: filtered ? '#1890ff' : undefined}}/>,
        onFilter: (value: string, record: any) =>
            record[dataIndex]
                ? record[dataIndex].toString().toLowerCase().includes(value.toLowerCase())
                : '',
        onFilterDropdownVisibleChange: (visible: boolean) => {
            if (visible) {
                setTimeout(() => searchInput.select(), 100);
            }
        },
        render: (text: string) => {
            return state.searchedColumn === dataIndex ? (
                <Highlighter
                    highlightStyle={{backgroundColor: '#ffc069', padding: 0}}
                    searchWords={[state.searchText]}
                    autoEscape
                    textToHighlight={text || ''}
                />
            ) : (
                text
            );},
    });

    const handleSearch = (selectedKeys: string[], confirm: () => void, dataIndex: string) => {
        confirm();
        dispatch({searchText: selectedKeys[0], searchedColumn: dataIndex});
        state.searchText = selectedKeys[0];
        state.searchedColumn = dataIndex;
    };

    const handleReset = (clearFilters: () => void) => {
        clearFilters();
        dispatch({searchText: ''});
        state.searchText = '';
    };

    const getTbProps = () => ({
        columns: [
            {
                title: intl.formatMessage({id: 'NAME'}),
                dataIndex: 'name',
                key: 'name',
                ellipsis: true,
                sorter: (a: ServerRunning, b: ServerRunning) => a.name.localeCompare(b.name),
                sortDirections: ['descend', 'ascend'],
                ...getColumnSearchProps('name')
            },
            {
                title: intl.formatMessage({id: 'STATUS'}),
                dataIndex: 'status',
                key: 'status',
                ellipsis: true,
                width: 120,
                filters: [
                    {text: intl.formatMessage({id: 'STOPPED'}), value: 'STOPPED'},
                    {text: intl.formatMessage({id: 'RUNNING'}), value: 'RUNNING'},
                    {text: intl.formatMessage({id: 'STARTING'}), value: 'STARTING'},
                    {text: intl.formatMessage({id: 'STOPPING'}), value: 'STOPPING'},
                ],
                onFilter: (value: string, record: ServerRunning) => record.status === value,
                render: (text: string) => translateStatus(text),
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
    const translateStatus = (status: string) => {
        let tag;
        const s = intl.formatMessage({id: status});
        switch (status) {
            case JarBootConst.STATUS_STARTED:
                tag = <Tag icon={<CaretRightOutlined/>} color={"success"}>{s}</Tag>
                break;
            case JarBootConst.STATUS_STOPPED:
                tag = <Tag icon={<PoweroffOutlined/>} color={"volcano"}>{s}</Tag>
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

    const getRowSelection = () => {
        return {
            type: 'checkbox',
            onChange: (selectedRowKeys: string[], selectedRows: ServerRunning[]) => {
                let current = state.current;
                if (!selectedRows || selectedRows.length <= 0) {
                    current = '';
                } else {
                    current = '' === current ? selectedRowKeys[0] : current;
                }
                dispatch({current, selectedRowKeys: selectedRowKeys, selectRows: selectedRows,});
            },
            selectedRowKeys: state.selectedRowKeys,
        };
    }

    const onRow = (record: ServerRunning) => ({
        onClick: () => dispatch({selectedRowKeys: [record.sid], selectRows: [record], current: record.sid})
    });

    const refreshServerList = (init: boolean = false) => {
        dispatch({loading: true});
        ServerMgrService.getServerList((resp: any) => {
            dispatch((preState: any) => {
                if (resp.resultCode < 0) {
                    CommonNotice.errorFormatted(resp);
                    return {loading: false};
                }
                const data = resp.result as ServerRunning[];
                const index = data.findIndex(item => preState.current === item.sid);
                if (init || -1 === index) {
                    //初始化选中第一个
                    let current = '';
                    let selectedRowKeys = [] as string[];
                    let selectRows: any = [];
                    if (data.length > 0) {
                        const first: ServerRunning = data[0];
                        current = first.sid;
                        selectedRowKeys = [first.sid];
                        selectRows = [first];
                    }
                    return {data, current, selectedRowKeys, selectRows, loading: false};
                }
                return {data, loading: false};
            });
        });
    };

    const finishCallback = (resp: any) => {
        dispatch({loading: false});
        if (0 !== resp.resultCode) {
            CommonNotice.errorFormatted(resp);
        }
    };

    const startServer = () => {
        if (state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }
        dispatch({loading: true});
        state.selectRows.forEach(clearDisplay);
        ServerMgrService.startServer(state.selectRows, finishCallback);
    };

    const clearDisplay = (server: ServerRunning) => {
        pubsub.publish(server.sid, JarBootConst.CLEAR_CONSOLE);
    };

    const stopServer = () => {
        if (state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }

        dispatch({loading: true});

        ServerMgrService.stopServer(state.selectRows, finishCallback);
    };

    const restartServer = () => {
        if (state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }
        dispatch({loading: true});
        state.selectRows.forEach(clearDisplay);
        ServerMgrService.restartServer(state.selectRows, finishCallback);
    };

    const getTbBtnProps = () => ([
        {
            name: 'Start',
            key: 'start ',
            icon: <CaretRightFilled className={styles.toolButtonGreenStyle}/>,
            onClick: startServer,
            disabled: !state.selectRows?.length
        },
        {
            name: 'Stop',
            key: 'stop',
            icon: <PoweroffOutlined className={styles.toolButtonRedStyle}/>,
            onClick: stopServer,
            disabled: !state.selectRows?.length
        },
        {
            name: 'Restart',
            key: 'restart',
            icon: <ReloadOutlined className={styles.toolButtonStyle}/>,
            onClick: restartServer,
            disabled: !state.selectRows?.length
        },
        {
            name: 'Refresh',
            key: 'refresh',
            icon: <SyncOutlined className={styles.toolButtonStyle}/>,
            onClick: () => refreshServerList(),
        },
        {
            name: 'New & update',
            key: 'upload',
            icon: <UploadOutlined className={styles.toolButtonStyle}/>,
            onClick: uploadFile,
        },
        {
            name: 'Delete',
            key: 'delete',
            icon: <DeleteOutlined className={styles.toolButtonRedStyle}/>,
            onClick: deleteServer,
            disabled: isDeleteDisabled()
        },
        {
            name: 'Dashboard',
            key: 'dashboard',
            icon: <DashboardOutlined className={styles.toolButtonRedStyle}/>,
            onClick: dashboardCmd,
            disabled: isCurrentNotRunning()
        }
    ]);

    const isDeleteDisabled = () => {
        if (1 !== state.selectRows?.length) {
            return true;
        }
        return JarBootConst.STATUS_STOPPED !== state.selectRows[0].status;
    };

    const deleteServer = () => {
        if (!state.selectRows?.length) {
            notSelectInfo();
            return;
        }
        if (state.selectRows?.length > 1) {
            CommonNotice.info("Select one to delete.");
            return;
        }
        Modal.confirm({
            title: intl.formatMessage({id: 'WARN'}),
            content: intl.formatMessage({id: 'DELETE_INFO'}),
            onOk: () => {
                ServerMgrService.deleteServer(state.selectRows[0].name).then(resp => {
                    if (0 !== resp.resultCode) {
                        CommonNotice.errorFormatted(resp);
                    }
                }).catch(CommonNotice.errorFormatted);
            }
        })
    };

    const isCurrentNotRunning = () => {
        const item = state.data.find((value: ServerRunning) => value.sid === state.current);
        return !(item && JarBootConst.STATUS_STARTED === item.status);
    }

    const uploadFile = () => {
        dispatch({uploadVisible: true});
    };

    const dashboardCmd = () => {
        if (state.selectRows?.length < 1 || StringUtil.isEmpty(state.current)) {
            notSelectInfo();
            return;
        }
        pubsub.publish(state.current, PUB_TOPIC.QUICK_EXEC_CMD, "dashboard");
    };

    const oneClickRestart = () => {
        pubsub.publish(state.current, JarBootConst.APPEND_LINE, "Restarting all...");
        disableOnClickButton();
        ServerMgrService.oneClickRestart();
    };

    const oneClickStart = () => {
        pubsub.publish(state.current, JarBootConst.APPEND_LINE, "Starting all...");
        disableOnClickButton();
        ServerMgrService.oneClickStart();
    };

    const oneClickStop = () => {
        pubsub.publish(state.current, JarBootConst.APPEND_LINE, "Stopping all...");
        disableOnClickButton();
        ServerMgrService.oneClickStop();
    };

    const disableOnClickButton = () => {
        if (state.oneClickLoading) {
            return;
        }
        dispatch({oneClickLoading: true});
        setTimeout(() => dispatch({oneClickLoading: false}), 5000);
    };

    const onUploadClose = () => {
        dispatch({uploadVisible: false});
        refreshServerList();
    };


    let tableOption: any = getTbProps();
    tableOption.scroll = {y: height};
    const loading = state.loading && 0 === state.data.length;
    return (<div>
        <div style={{display: 'flex'}}>
            <div style={{flex: 'inherit', width: '28%'}}>
                <CommonTable toolbarGap={5} option={tableOption}
                             toolbar={getTbBtnProps()} height={height}/>
                <OneClickButtons loading={state.oneClickLoading}
                                 oneClickRestart={oneClickRestart}
                                 oneClickStart={oneClickStart}
                                 oneClickStop={oneClickStop}/>
            </div>
            <div style={{flex: 'inherit', width: '72%'}}>
                {loading && <Result icon={<LoadingOutlined/>} title={intl.formatMessage({id: 'LOADING'})}/>}
                {<SuperPanel key={PUB_TOPIC.ROOT}
                             server={""}
                             sid={PUB_TOPIC.ROOT}
                             visible={!loading && state.current?.length <= 0}/>}
                {state.data.map((value: ServerRunning) => (
                    <SuperPanel key={value.sid}
                                server={value.name}
                                sid={value.sid}
                                visible={state.current === value.sid}/>
                ))}
            </div>
        </div>
        {state.uploadVisible && <UploadFileModal server={state.selectRows[0].name}
                                                 onOk={onUploadClose}
                                                 onCancel={() => dispatch({uploadVisible: false})}/>}
    </div>);
};

export default ServerMgrView;
