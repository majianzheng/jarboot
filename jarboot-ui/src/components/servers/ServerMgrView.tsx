import React, {useEffect, useReducer, useRef} from "react";
import {Button, Empty, Input, message, Modal, Result, Space, Spin, Tree} from "antd";
import ServerMgrService, {ServerRunning, TreeNode} from "@/services/ServerMgrService";
import CommonNotice, {notSelectInfo} from '@/common/CommonNotice';
import {
    AppstoreOutlined,
    CaretRightFilled,
    CaretRightOutlined,
    DashboardOutlined,
    LoadingOutlined,
    PoweroffOutlined,
    SearchOutlined,
    SyncOutlined,
    UploadOutlined
} from '@ant-design/icons';
import {JarBootConst, MsgData} from '@/common/JarBootConst';
import Logger from "@/common/Logger";
import {PUB_TOPIC, pubsub, SuperPanel} from "@/components/servers";
import BottomBar from "@/components/servers/BottomBar";
import CommonTable from "@/components/table";
import UploadFileModal from "@/components/servers/UploadFileModal";
import StringUtil from "@/common/StringUtil";
import styles from "./index.less";
import {useIntl} from "umi";
import ServerConfig from "@/components/servers/ServerConfig";
import {DeleteIcon, ExportIcon, ImportIcon, RestartIcon, StoppedIcon} from "@/components/icons";
import {DataNode, EventDataNode, Key} from "rc-tree/lib/interface";
import CloudService from "@/services/CloudService";
import CommonUtils from "@/common/CommonUtils";
// @ts-ignore
import Highlighter from 'react-highlight-words';
import IntlText from "@/common/IntlText";
import TopTitleBar from "@/components/servers/TopTitleBar";
import {CONSOLE_TOPIC} from "@/components/console";

interface ServerMgrViewState {
    loading: boolean;
    data: ServerRunning[];
    treeData: TreeNode[];
    uploadVisible: boolean;
    selectedRowKeys: string[];
    searchText: string;
    searchedColumn: string;
    selectRows: ServerRunning[];
    current: string;
    sideView: 'tree' | 'list';
    contentView: 'config' | 'console';
    importing: boolean;
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
        selectRows: [] as ServerRunning[],
        current: '',
        sideView: getInitSideView(),
        contentView: JarBootConst.CONFIG_VIEW as ('config'|'console'),
        importing: false,
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
            const item = preState?.data?.find((value: ServerRunning) => value.sid === data.sid);
            if (!item || item.status === data.body) {
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
                    pubsub.publish(key, CONSOLE_TOPIC.CLEAR_CONSOLE);
                    pubsub.publish(key, CONSOLE_TOPIC.START_LOADING);
                    break;
                case JarBootConst.STATUS_STOPPING:
                    Logger.log(`${server} 停止中...`);
                    pubsub.publish(key, CONSOLE_TOPIC.START_LOADING);
                    break;
                case JarBootConst.STATUS_STARTED:
                    Logger.log(`${server} 已启动`);
                    pubsub.publish(key, CONSOLE_TOPIC.FINISH_LOADING);
                    pubsub.publish(key, PUB_TOPIC.FOCUS_CMD_INPUT);
                    break;
                case JarBootConst.STATUS_STOPPED:
                    Logger.log(`${server} 已停止`);
                    pubsub.publish(key, CONSOLE_TOPIC.FINISH_LOADING);
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
                title: <IntlText id={"NAME"}/>,
                dataIndex: 'name',
                key: 'name',
                ellipsis: true,
                sorter: (a: ServerRunning, b: ServerRunning) => a.name.localeCompare(b.name),
                sortDirections: ['descend', 'ascend'],
                ...getColumnSearchProps('name')
            },
            {
                title: <IntlText id={"GROUP"}/>,
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
        switch (status) {
            case JarBootConst.STATUS_STARTED:
                tag = <CaretRightOutlined className={styles.statusRunning}/>;
                break;
            case JarBootConst.STATUS_STOPPED:
                tag = <StoppedIcon className={styles.statusStopped}/>;
                break;
            case JarBootConst.STATUS_STARTING:
                tag = <LoadingOutlined className={styles.statusStarting}/>;
                break;
            case JarBootConst.STATUS_STOPPING:
                tag = <LoadingOutlined className={styles.statusStopping}/>;
                break;
            default:
                return undefined;
        }
        const title = intl.formatMessage({id: status});
        return <span title={title} className={styles.statusIcon}>{tag}</span>;
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

    const startSignal = (server: ServerRunning) => {
        if (server.isLeaf && JarBootConst.STATUS_STOPPED === server.status) {
            ServerMgrService.startServer([server], finishCallback);
        }
    };

    const onRow = (record: ServerRunning) => ({
        onClick: () => {
            dispatch({selectedRowKeys: [record.sid], selectRows: [record], current: record.sid});
            pubsub.publish(record.sid, PUB_TOPIC.FOCUS_CMD_INPUT);
        },
        onDoubleClick: () => startSignal(record)
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
                        let first: ServerRunning;
                        if (preState.sideView === JarBootConst.LIST_VIEW && treeData.length > 0) {
                            const firstChildren = treeData[0]?.children as ServerRunning[] || [];
                            first = (firstChildren[0] || data[0]);
                        } else {
                            first = data[0];
                        }
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
        ServerMgrService.startServer(state.selectRows, finishCallback);
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
        ServerMgrService.restartServer(state.selectRows, finishCallback);
    };

    const getToolBtnProps = () => ([
        {
            title: intl.formatMessage({id: 'START'}),
            key: 'start ',
            icon: <CaretRightFilled className={styles.toolButtonIcon}/>,
            onClick: startServer,
            disabled: !state.selectRows?.length
        },
        {
            title: intl.formatMessage({id: 'STOP'}),
            key: 'stop',
            icon: <PoweroffOutlined className={styles.toolButtonRedIcon}/>,
            onClick: stopServer,
            disabled: !state.selectRows?.length
        },
        {
            title: intl.formatMessage({id: 'RESTART'}),
            key: 'restart',
            icon: <RestartIcon className={styles.toolButtonIcon}/>,
            onClick: restartServer,
            disabled: !state.selectRows?.length
        },
        {
            title: intl.formatMessage({id: 'REFRESH_BTN'}),
            key: 'refresh',
            icon: <SyncOutlined className={styles.toolButtonIcon}/>,
            onClick: () => refreshServerList(),
        },
        {
            title: intl.formatMessage({id: 'UPLOAD_NEW'}),
            key: 'upload',
            icon: <UploadOutlined className={styles.toolButtonIcon}/>,
            onClick: uploadFile,
        },
        {
            title: intl.formatMessage({id: 'DELETE'}),
            key: 'delete',
            icon: <DeleteIcon className={styles.toolButtonRedIcon}/>,
            onClick: deleteServer,
            disabled: isDeleteDisabled()
        },
        {
            title: intl.formatMessage({id: 'DASHBOARD'}),
            key: 'dashboard',
            icon: <DashboardOutlined className={styles.toolButtonRedIcon}/>,
            onClick: dashboardCmd,
            disabled: isCurrentNotRunning()
        },
        {
            title: intl.formatMessage({id: 'EXPORT'}),
            key: 'export',
            icon: <ExportIcon className={styles.toolButtonIcon}/>,
            onClick: onExport,
            disabled: (1 !== state.selectRows?.length || !state.selectRows[0].isLeaf)
        },
        {
            title: intl.formatMessage({id: 'IMPORT'}),
            key: 'import',
            icon: state.importing ? <LoadingOutlined className={styles.toolButtonIcon}/> : <ImportIcon className={styles.toolButtonIcon}/>,
            onClick: onImport,
            disabled: state.importing,
        }
    ]);

    const onExport = () => {
        if (1 !== state.selectRows?.length) {
            return;
        }
        const name = state.selectRows[0].name;
        if (StringUtil.isEmpty(name)) {
            return;
        }
        Modal.confirm({
            title: `${intl.formatMessage({id: 'EXPORT'})} ${name}?`,
            onOk: () => CommonUtils.exportServer(name)
        });
    };

    const onImport = () => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'application/zip';
        input.onchange = () => {
            if (!input.files?.length) {
                return;
            }
            const file = input.files[0];
            dispatch({importing: true});
            const content = intl.formatMessage({id: 'START_UPLOAD_INFO'}, {name: file.name});
            CommonNotice.info(content);
            const key = file.name.replace('.zip', '');
            message.loading({content, key, duration: 0}, 0).then(() => {});
            CloudService.pushServerDirectory(file).then(resp => {
                if (0 !== resp.resultCode) {
                    CommonNotice.errorFormatted(resp);
                }
                dispatch({importing: false});
            }).catch(error => {
                CommonNotice.errorFormatted(error);
                dispatch({importing: false});
            });
        };
        input.click();
    };

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
        const treeData = [] as ServerRunning[];
        const groupMap = new Map<string, ServerRunning[]>();
        data.forEach(server => {
            const group = server.group || '';
            let children = groupMap.get(group);
            if (!children) {
                children = [] as ServerRunning[];
                groupMap.set(group, children);
                const title = (<span className={styles.groupRow}>
                    {group.length ? group : <IntlText id={'DEFAULT_GROUP'}/>}
                </span>);
                treeData.push({
                    title,
                    sid: group,
                    key: group,
                    group,
                    icon: groupIcon,
                    name: "", path: "", status: "", children});
            }
            const child = server as ServerRunning;
            child.key = server.sid;
            child.title = <span onDoubleClick={() => startSignal(server)}>{server.name}</span>;
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
                {getToolBtnProps().map(props => (<Button className={styles.treeToolbarBtn} type={"text"} {...props}/>))}
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
        const configView = JarBootConst.CONSOLE_VIEW === state.contentView;
        //按钮为控制台，则当前为服务配置
        let server = {} as ServerRunning;
        let configTitle = '';
        if (configView) {
            server = state.data.find(value => state.current === value.sid) as ServerRunning;
            const conf = intl.formatMessage({id: 'SERVICES_CONF'});
            if (server) {
                configTitle = `${server?.name || ''} - ${conf}`;
            } else {
                configTitle = `- ${conf}`;
            }
        }
        const closeConfig = () => onViewChange(JarBootConst.CONTENT_VIEW, JarBootConst.CONFIG_VIEW);
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
            <div style={{display: configView ? 'block' : 'none', background: '#FAFAFA'}}>
                <TopTitleBar title={configTitle}
                             onClose={closeConfig}/>
                <ServerConfig path={server?.path || ''}
                              sid={state.current || ''}
                              group={server?.group || ''}
                              onClose={closeConfig}
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
