import {Form, Select, Layout, Modal, Upload, Button} from 'antd';
import {useEffect, useState} from "react";
import CommonTable from "@/components/table";
import {formatMsg} from "@/common/IntlFormat";
import {DeleteOutlined, SyncOutlined, UploadOutlined} from "@ant-design/icons";
import PluginsService from "@/services/PluginsService";
import CommonNotice from "@/common/CommonNotice";
import {useIntl} from "umi";
import CommonUtils from "@/common/CommonUtils";

const { Sider } = Layout;
const height = window.innerHeight - 130;
const toolButtonStyle = {color: '#1890ff', fontSize: '18px'};
const toolButtonRedStyle = {color: 'red', fontSize: '18px'};
const layout = {
    labelCol: {span: 8},
    wrapperCol: {span: 16},
};

const PluginsManager = () => {
    const intl = useIntl();
    const [form] = Form.useForm();
    let [collapsed, setCollapsed] = useState(false);
    let [loading, setLoading] = useState(true);
    let [data, setData] = useState([]);
    let [selected, setSelected] = useState({keys: [] as any[], rows: [] as any[]});
    let [visible, setVisible] = useState(false);

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

    const removePlugin = () => {
        const type = selected.rows[0]?.type as string;
        const filename = selected.rows[0]?.fileName as string;
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
                setSelected({keys: [record.id], rows: [record]});
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
            rowKey: 'id',
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
                onClick: () => setVisible(true),
            },
            {
                name: 'Dashboard',
                key: 'dashboard',
                icon: <DeleteOutlined style={toolButtonRedStyle}/>,
                onClick: removePlugin,
            }
        ]
    };

    const formData = () => {
        return {
            type: form.getFieldValue("type"),
        }
    }

    const props = {
        name: 'file',
        action: `/api/jarboot/plugins`,
        data: formData,
        headers: {Authorization: CommonUtils.getToken()},
        onChange(info: any) {
            if (info.file.status === 'done') {
                const type = form.getFieldValue("type");
                CommonNotice.success(`${info.file.name} file uploaded successfully, restart ${type} to effect`);
            } else if (info.file.status === 'error') {
                CommonNotice.error(`${info.file.name} file upload failed.`);
            }
        },
    };

    let tableOption: any = _getTbProps();
    tableOption.scroll = { y: height};
    const plugin = selected.rows[0];
    return (
        <Layout>
            <Sider width={300} collapsible collapsed={collapsed} onCollapse={onCollapse}>
                <CommonTable toolbarGap={5} option={tableOption}
                             toolbar={_getTbBtnProps()} height={height}/>
            </Sider>
            <Layout>
                {plugin && <iframe frameBorder={0}
                                       width={'100%'}
                                       height={'100%'}
                                       src={`/plugins/page/${plugin.type}/${plugin.fileName}/index.html`}/>}
            </Layout>
            {visible && <Modal title={intl.formatMessage({id: 'PLUGIN_UPLOAD_TITLE'})}
                               visible={true}
                               width={860}
                               destroyOnClose={true}
                               onOk={() => setVisible(false)}
                               onCancel={() => setVisible(false)}>
                <Form {...layout} form={form} initialValues={{type: 'server'}}>
                    <Form.Item label={intl.formatMessage({id: 'TYPE'})} name={"type"}>
                        <Select>
                            <Select.Option value={"server"}>server</Select.Option>
                            <Select.Option value={"agent"}>agent</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item label={intl.formatMessage({id: 'PLUGIN_FILE'})} name={"file"}>
                        <Upload {...props}>
                            <Button icon={<UploadOutlined />}>{intl.formatMessage({id: 'UPLOAD_BUTTON'})}</Button>
                        </Upload>
                    </Form.Item>
                </Form>
            </Modal>}
        </Layout>
    );
};

export default PluginsManager;
