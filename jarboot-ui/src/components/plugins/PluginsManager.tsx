import {Layout, Modal} from 'antd';
import {useEffect, useState} from "react";
import CommonTable from "@/components/table";
import {formatMsg} from "@/common/IntlFormat";
import {
    DeleteOutlined,
    SyncOutlined,
    UploadOutlined
} from "@ant-design/icons";
import PluginsService from "@/services/PluginsService";
import CommonNotice from "@/common/CommonNotice";

const { Sider } = Layout;
const height = window.innerHeight - 130;
const toolButtonStyle = {color: '#1890ff', fontSize: '18px'};
const toolButtonRedStyle = {color: 'red', fontSize: '18px'};

const PluginsManager = () => {
    let [collapsed, setCollapsed] = useState(false);
    let [loading, setLoading] = useState(true);
    let [data, setData] = useState([]);
    let [selected, setSelected] = useState({keys: [] as any[], rows: [] as any[]});

    useEffect(() => {
        query();
    }, []);
    const onCollapse = (value: boolean) => {
        setCollapsed(value);
    };
    const query = () => {
        setLoading(true);
        PluginsService.getAgentPlugins().then(resp => {
            setLoading(false);
            if (resp.resultCode < 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            setData(resp.result);
        }).catch(CommonNotice.errorFormatted);
    };
    const uploadPlugin = () => {
        //
    };

    const removePlugin = () => {
        const type = selected.rows[0]?.type as string;
        const filename = selected.rows[0]?.filename as string;
        if (!type || !filename) {
            return;
        }
        Modal.confirm({
            title: `Remove plugin ${filename} or not?`,
            onOk() {
                PluginsService.removePlugin(type, filename).finally(() => {
                    query();
                });
            }
        })
    };

    const _getRowSelection = () => {
        return {
            type: 'radio',
            onChange: (keys: any, rows: any) => setSelected({keys, rows}),
            selectedRowKeys: selected.keys,
        };
    };

    const _onRow = (record: any) => {
        return {
            onClick: () => {
                setSelected({keys: [record.name], rows: [record]});
            },
        };
    };

    const _getTbProps = () => {
        return {
            columns: [
                {
                    title: formatMsg('NAME'),
                    dataIndex: 'name',
                    key: 'name',
                    ellipsis: true,
                },
                {
                    title: formatMsg('TYPE'),
                    dataIndex: 'type',
                    key: 'type',
                    ellipsis: true,
                    width: 120,
                    visible: false,
                },
            ],
            loading: loading,
            dataSource: data,
            pagination: false,
            rowKey: 'name',
            size: 'small',
            rowSelection: _getRowSelection(),
            onRow: _onRow,
            showHeader: true,
            scroll: height,
        };
    }

    const _getTbBtnProps = () => {
        if (collapsed) {
            return [];
        }
        return [
            {
                name: 'Refresh',
                key: 'refresh',
                icon: <SyncOutlined style={toolButtonStyle}/>,
                onClick: query,
            },
            {
                name: 'New & update',
                key: 'upload',
                icon: <UploadOutlined style={toolButtonStyle}/>,
                onClick: uploadPlugin,
            },
            {
                name: 'Dashboard',
                key: 'dashboard',
                icon: <DeleteOutlined style={toolButtonRedStyle}/>,
                onClick: removePlugin,
            }
        ]
    };

    let tableOption: any = _getTbProps();
    tableOption.scroll = { y: height};
    return (
        <Layout>
            <Sider width={300} collapsible collapsed={collapsed} onCollapse={onCollapse}>
                <CommonTable toolbarGap={5} option={tableOption}
                             toolbar={_getTbBtnProps()} height={height}/>
            </Sider>
            <Layout>
            </Layout>
        </Layout>
    );
}

export default PluginsManager;
