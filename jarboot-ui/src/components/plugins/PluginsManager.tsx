import {Form, Select, Layout, Modal, Upload, Button} from 'antd';
import {useEffect, useState} from "react";
import CommonTable from "@/components/table";
import {formatMsg} from "@/common/IntlFormat";
import {DeleteOutlined, SyncOutlined, UploadOutlined, ArrowRightOutlined} from "@ant-design/icons";
import PluginsService from "@/services/PluginsService";
import CommonNotice from "@/common/CommonNotice";
import {useIntl} from "umi";
import CommonUtils from "@/common/CommonUtils";

const { Sider } = Layout;
const height = window.innerHeight - 130;
const toolButtonStyle = {color: '#1890ff', fontSize: '18px'};
const toolButtonRedStyle = {color: 'red', fontSize: '18px'};
const layout = {
    labelCol: {span: 6},
    wrapperCol: {span: 18},
};

const PluginsManager = () => {
    const intl = useIntl();
    const [form] = Form.useForm();
    let [loading, setLoading] = useState(true);
    let [data, setData] = useState([]);
    let [selected, setSelected] = useState({keys: [] as any[], rows: [] as any[]});
    let [visible, setVisible] = useState(false);

    useEffect(() => {
        query();
    }, []);
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
                name: 'Delete',
                key: 'delete',
                icon: <DeleteOutlined style={toolButtonRedStyle}/>,
                onClick: removePlugin,
            },
            {
                name: 'Open new window',
                key: 'open',
                icon: <ArrowRightOutlined style={toolButtonStyle}/>,
                onClick: () => {
                    if (url) {
                        window.open(url, selected.rows[0].id);
                    }
                },
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

    const getUrl = () => {
        const plugin = selected.rows[0];
        if (plugin) {
            return `/plugins/page/${plugin.type}/${plugin.fileName}/index.html`;
        } else {
            return null;
        }
    };

    let tableOption: any = _getTbProps();
    tableOption.scroll = { y: height};
    const url = getUrl();
    return (
        <Layout>
            <Sider width={300}>
                <CommonTable toolbarGap={5} option={tableOption}
                             toolbar={_getTbBtnProps()} height={height}/>
            </Sider>
            <Layout>
                {url && <iframe frameBorder={0} width={'100%'} height={'100%'} src={url}/>}
            </Layout>
            {visible && <Modal title={intl.formatMessage({id: 'PLUGIN_UPLOAD_TITLE'})}
                               visible={true} maskClosable={false}
                               width={600}
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
