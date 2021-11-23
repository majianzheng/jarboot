import React, {useEffect, useReducer, useRef} from "react";
import {Result, Input, Space, Button, Modal, Empty, Tree, Spin} from "antd";
import ServerMgrService, {ServerRunning, TreeNode} from "@/services/ServerMgrService";
import CommonNotice, {notSelectInfo} from '@/common/CommonNotice';
import {
    SyncOutlined, CaretRightOutlined, CaretRightFilled, DashboardOutlined,
    PoweroffOutlined, ReloadOutlined, UploadOutlined, LoadingOutlined, SearchOutlined, AppstoreOutlined
} from '@ant-design/icons';
import { JarBootConst, MsgData } from '@/common/JarBootConst';
import Logger from "@/common/Logger";
import {PUB_TOPIC, SuperPanel, pubsub} from "@/components/servers";
import BottomBar from "@/components/servers/BottomBar";
import CommonTable from "@/components/table";
import UploadFileModal from "@/components/servers/UploadFileModal";
import StringUtil from "@/common/StringUtil";
import styles from "./index.less";
import {useIntl} from "umi";
import ServerConfig from "@/components/setting/ServerConfig";
import {DeleteIcon, StoppedIcon} from "@/components/icons";
import {DataNode, EventDataNode, Key} from "rc-tree/lib/interface";
// @ts-ignore
import Highlighter from 'react-highlight-words';

interface ServerMgrViewState {
    loading: boolean;
    data: ServerRunning[];
    treeData: TreeNode[];
    uploadVisible: boolean;
    selectedRowKeys: string[];
    searchText: string;
    searchedColumn: string;
    selectRows: TreeNode[];
    current: string;
    sideView: 'tree' | 'list';
    contentView: 'config' | 'console';
}

let searchInput = {} as any;

const getInitSideView = (): 'tree' | 'list' => {
    const sideView = localStorage.getItem(JarBootConst.SIDE_VIEW);
    return (sideView || JarBootConst.LIST_VIEW) as ('tree' | 'list');
};

const ServerMgrView = () => {
    const intl = useIntl();
    const treeRef = useRef<any>();
    const initArg: ServerMgrViewState = {
        loading: false,
        data: [] as ServerRunning[],
        treeData: [] as TreeNode[],
        uploadVisible: false,
        selectedRowKeys: [] as string[],
        searchText: '',
        searchedColumn: '',
        selectRows: [] as TreeNode[],
        current: '',
        sideView: getInitSideView(),
        contentView: JarBootConst.CONFIG_VIEW as ('config'|'console')
    };
    const [state, dispatch] = useReducer((state: ServerMgrViewState, action: any) => {
        if ('function' === typeof action) {
            return {...state, ...action(state)} as ServerMgrViewState;
        }
        return {...state, ...action} as ServerMgrViewState;
    }, initArg, (arg): ServerMgrViewState => ({...arg} as ServerMgrViewState));

    const height = window.innerHeight - 86;

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
        dispatch((preState: ServerMgrViewState) => {
            let data: ServerRunning[] = preState.data || [];
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
        dispatch((preState: ServerMgrViewState) => {
            const item = preState?.data?.find((value: ServerRunning) => value.sid === data.sid) as TreeNode;
            if (!item) {
                return {};
            }
            const key = data.sid;
            const server = item.name as string;
            item.status = data.body;
            switch (item.status) {
                case JarBootConst.STATUS_STARTING:
                    // 激活终端显示
                    activeConsole(key);
                    Logger.log(`${server} 启动中...`);
                    pubsub.publish(key, JarBootConst.START_LOADING);
                    clearDisplay(item);
                    break;
                case JarBootConst.STATUS_STOPPING:
                    Logger.log(`${server} 停止中...`);
                    pubsub.publish(key, JarBootConst.START_LOADING);
                    item.status = JarBootConst.STATUS_STOPPING;
                    break;
                case JarBootConst.STATUS_STARTED:
                    Logger.log(`${server} 已启动`);
                    pubsub.publish(key, JarBootConst.FINISH_LOADING);
                    pubsub.publish(key, PUB_TOPIC.FOCUS_CMD_INPUT);
                    break;
                case JarBootConst.STATUS_STOPPED:
                    Logger.log(`${server} 已停止`);
                    pubsub.publish(key, JarBootConst.FINISH_LOADING);
                    break;
                default:
                    return {};
            }
            item.icon = translateStatus(item.status);
            return {data: [...preState.data], treeData: [...preState.treeData]};
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
        render: (text: string, row: ServerRunning) => {
            const s = state.searchedColumn === dataIndex ? (
                <Highlighter
                    highlightStyle={JarBootConst.HIGHLIGHT_STYLE}
                    searchWords={[state.searchText]}
                    autoEscape
                    textToHighlight={text || ''}
                />
            ) : (
                text
            );
            if ('name' === dataIndex) {
                return (<>{translateStatus(row.status)}{s}</>);
            }
            return s;
        },
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
                title: intl.formatMessage({id: 'GROUP'}),
                dataIndex: 'group',
                key: 'group',
                ellipsis: true,
                width: 120,
                sorter: (a: ServerRunning, b: ServerRunning) => a.group.localeCompare(b.group),
                sortDirections: ['descend', 'ascend'],
                ...getColumnSearchProps('group')
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
        const title = intl.formatMessage({id: status});
        switch (status) {
            case JarBootConst.STATUS_STARTED:
                tag = <CaretRightOutlined title={title} style={{color: '#52c41a'}}/>;
                break;
            case JarBootConst.STATUS_STOPPED:
                tag = <StoppedIcon title={title} style={{color: '#C16666', opacity: 0.5}}/>;
                break;
            case JarBootConst.STATUS_STARTING:
                tag = <LoadingOutlined title={title} style={{color: '#2db7f5'}}/>;
                break;
            case JarBootConst.STATUS_STOPPING:
                tag = <LoadingOutlined title={title} style={{color: '#d4380d'}}/>;
                break;
            default:
                return undefined;
        }
        return <span className={styles.statusIcon}>{tag}</span>;
    };

    const getRowSelection = () => ({
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
    });

    const onRow = (record: ServerRunning) => ({
        onClick: () => {
            dispatch({selectedRowKeys: [record.sid], selectRows: [record], current: record.sid});
            pubsub.publish(record.sid, PUB_TOPIC.FOCUS_CMD_INPUT);
        }
    });

    const refreshServerList = (init: boolean = false) => {
        dispatch({loading: true});
        ServerMgrService.getServerList((resp: any) => {
            dispatch((preState: ServerMgrViewState) => {
                if (resp.resultCode < 0) {
                    CommonNotice.errorFormatted(resp);
                    return {loading: false};
                }
                const data = resp.result as ServerRunning[];
                const index = data.findIndex(item => preState.current === item.sid);
                const treeData = getTreeData(data);
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
                    return {data, treeData, current, selectedRowKeys, selectRows, loading: false};
                }
                return {data, treeData, loading: false};
            });
        });
    };

    const finishCallback = (resp: any) => {
        if (0 !== resp.resultCode) {
            CommonNotice.errorFormatted(resp);
        }
    };

    const startServer = () => {
        if (state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }
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

        ServerMgrService.stopServer(state.selectRows, finishCallback);
    };

    const restartServer = () => {
        if (state.selectRows.length < 1) {
            notSelectInfo();
            return;
        }
        state.selectRows.forEach(clearDisplay);
        ServerMgrService.restartServer(state.selectRows, finishCallback);
    };

    const getToolBtnProps = () => ([
        {
            name: 'Start',
            key: 'start ',
            icon: <CaretRightFilled className={styles.toolButtonIcon}/>,
            onClick: startServer,
            disabled: !state.selectRows?.length
        },
        {
            name: 'Stop',
            key: 'stop',
            icon: <PoweroffOutlined className={styles.toolButtonRedIcon}/>,
            onClick: stopServer,
            disabled: !state.selectRows?.length
        },
        {
            name: 'Restart',
            key: 'restart',
            icon: <ReloadOutlined className={styles.toolButtonIcon}/>,
            onClick: restartServer,
            disabled: !state.selectRows?.length
        },
        {
            name: 'Refresh',
            key: 'refresh',
            icon: <SyncOutlined className={styles.toolButtonIcon}/>,
            onClick: () => refreshServerList(),
        },
        {
            name: 'New & update',
            key: 'upload',
            icon: <UploadOutlined className={styles.toolButtonIcon}/>,
            onClick: uploadFile,
        },
        {
            name: 'Delete',
            key: 'delete',
            icon: <DeleteIcon className={styles.toolButtonRedIcon}/>,
            onClick: deleteServer,
            disabled: isDeleteDisabled()
        },
        {
            name: 'Dashboard',
            key: 'dashboard',
            icon: <DashboardOutlined className={styles.toolButtonRedIcon}/>,
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
        });
    };

    const isCurrentNotRunning = () => {
        const item = state.data.find((value: ServerRunning) => value.sid === state.current);
        return !(item && JarBootConst.STATUS_STARTED === item.status);
    };

    const uploadFile = () => dispatch({uploadVisible: true});

    const dashboardCmd = () => {
        if (state.selectRows?.length < 1 || StringUtil.isEmpty(state.current)) {
            notSelectInfo();
            return;
        }
        pubsub.publish(state.current, PUB_TOPIC.QUICK_EXEC_CMD, "dashboard");
    };

    const onUploadClose = () => {
        dispatch({uploadVisible: false});
        refreshServerList();
    };

    const onViewChange = (key: string, value: string) => {
        dispatch({[key]: value});
        localStorage.setItem(key, value);
    };

    const onGroupChanged = (sid: string, group: string) => {
        dispatch((preState: any) => {
            const data = preState.data as ServerRunning[];
            const server = data.find(value => value.sid === sid);
            if (server) {
                server.group = group;
                const treeData = getTreeData(data);
                return {data, treeData};
            }
            return {};
        });
    };

    const onTreeSelect = (selectedRowKeys: Key[], info: {
        event: 'select';
        selected: boolean;
        node: EventDataNode;
        selectedNodes: DataNode[];
        nativeEvent: MouseEvent;
    }) => {
        dispatch((preState: ServerMgrViewState) => {
            const selectRows = info.selectedNodes;
            if (info.node.isLeaf) {
                const current = info.node.key as string;
                pubsub.publish(current, PUB_TOPIC.FOCUS_CMD_INPUT);
                return {selectedRowKeys, selectRows, current};
            }
            preState?.current?.length && pubsub.publish(preState.current, PUB_TOPIC.FOCUS_CMD_INPUT);
            return {selectedRowKeys, selectRows};
        });
    };

    const groupIcon = (): React.ReactNode => {
        return (<AppstoreOutlined className={styles.groupIcon}/>);
    };

    const getTreeData = (data: ServerRunning[]): TreeNode[] => {
        const treeData = [] as TreeNode[];
        const groupMap = new Map<string, TreeNode[]>();
        data.forEach(server => {
            const group = server.group || '';
            let children = groupMap.get(group);
            if (!children) {
                children = [] as TreeNode[];
                groupMap.set(group, children);
                treeData.push({
                    title: group,
                    sid: group,
                    key: group,
                    group,
                    icon: groupIcon,
                    name: "", path: "", status: "", children});
            }
            const child = server as TreeNode;
            child.key = server.sid;
            child.title = server.name;
            child.isLeaf = true;
            child.icon = translateStatus(server.status);
            children.push(child);
        });
        return treeData.sort((a, b) => a.group.localeCompare(b.group));
    };

    const onTreeSearch = (searchText: string) => {
        dispatch((preState: ServerMgrViewState) => {
            if (searchText?.length) {
                const text = searchText.toLowerCase();
                const item = preState.data.find(value =>
                    (value.name.toLowerCase().includes(text) || value.group.toLowerCase().includes(text)));
                item && treeRef.current?.scrollTo({key: item.sid});
            }
            return {searchText};
        });
    };

    const treeTitleRender = (node: DataNode) => {
        return (state.searchText?.length ? <Highlighter
            highlightStyle={JarBootConst.HIGHLIGHT_STYLE}
            searchWords={[state.searchText]}
            autoEscape
            textToHighlight={node.title || ''}
        /> : node.title);
    };

    const treeView = () => (<div>
        <div style={{height: height - 6}} className={styles.serverTree}>
            <div className={styles.treeToolbar}>
                {getToolBtnProps().map(props => (<Button style={{marginBottom: 6, width: '100%'}} type={"text"} {...props}/>))}
            </div>
            <div style={{height: height - 10}} className={styles.treeContent}>
                <Spin spinning={state.loading}>
                    {state.data?.length ? <div>
                        <Input.Search placeholder="Input name to search"
                                      style={{padding: "0 2px 0 2px"}}
                                      onSearch={onTreeSearch} allowClear enterButton/>
                        <Tree.DirectoryTree multiple className={styles.treeView}
                                            ref={treeRef}
                                            defaultExpandAll
                                            titleRender={treeTitleRender}
                                            expandAction={"doubleClick"}
                                            selectedKeys={state.selectedRowKeys}
                                            onSelect={onTreeSelect}
                                            treeData={state.treeData}/>
                    </div> : <Empty/>}
                </Spin>
            </div>
        </div>
    </div>);

    const sideView = () => {
        let tableOption: any = getTbProps();
        tableOption.scroll = {y: height};
        return (<>
            <div style={{display: JarBootConst.TREE_VIEW === state.sideView ? 'block' : 'none'}}
                 className={styles.serverTable}>
                <CommonTable option={tableOption}
                             toolbar={getToolBtnProps()} height={height}/>
            </div>
            <div style={{display: JarBootConst.LIST_VIEW === state.sideView ? 'block' : 'none'}}>
                {treeView()}
            </div>
        </>);
    };

    const contentView = () => {
        //按钮为控制台，则当前为服务配置
        const server = state.selectRows?.length ? state.selectRows[0] : null;
        return (<div>
            <div style={{display: JarBootConst.CONFIG_VIEW === state.contentView ? 'block' : 'none'}}>
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
            <div style={{display: JarBootConst.CONSOLE_VIEW === state.contentView ? 'block' : 'none', background: '#FAFAFA'}}>
                <ServerConfig path={server?.path || ''}
                              sid={server?.sid || ''}
                              group={server?.group || ''}
                              onClose={() => onViewChange(JarBootConst.CONTENT_VIEW, JarBootConst.CONFIG_VIEW)}
                              onGroupChanged={onGroupChanged}/>
            </div>
        </div>);
    };


    let tableOption: any = getTbProps();
    tableOption.scroll = {y: height};
    const loading = state.loading && 0 === state.data.length;
    return (<div>
        <div className={styles.serverMgr}>
            <div className={styles.serverMgrSide}>
                {sideView()}
                <BottomBar sideView={state.sideView} contentView={state.contentView} onViewChange={onViewChange}/>
            </div>
            <div className={styles.serverMgrContent}>
                {loading && <Result icon={<LoadingOutlined/>} title={intl.formatMessage({id: 'LOADING'})}/>}
                {contentView()}
            </div>
        </div>
        {state.uploadVisible && <UploadFileModal server={state.selectRows.length > 0 ? state.selectRows[0].name : ''}
                                                 onOk={onUploadClose}
                                                 onCancel={() => dispatch({uploadVisible: false})}/>}
    </div>);
};

export default ServerMgrView;
