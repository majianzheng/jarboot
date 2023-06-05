import {Form, Select, Layout, Modal, Upload, Button} from 'antd';
import React, {useEffect, useState} from "react";
import CommonTable from "@/components/table";
import {SyncOutlined, UploadOutlined, ArrowRightOutlined} from "@ant-design/icons";
import PluginsService from "@/services/PluginsService";
import CommonNotice from "@/common/CommonNotice";
import {useIntl} from "umi";
import CommonUtils from "@/common/CommonUtils";
import {DeleteIcon} from "@/components/icons";
import styles from "@/common/global.less";
import IntlText from "@/common/IntlText";

const { Sider } = Layout;
const height = window.innerHeight - 70;
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

    useEffect(query, []);

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

    const getRowSelection = () => ({
        type: 'radio',
        onChange: (keys: any, rows: any) => setSelected({keys, rows}),
        selectedRowKeys: selected.keys,
    });

    const onRow = (record: any) => ({
        onClick: () => {
            setSelected({keys: [record.id], rows: [record]});
        },
    });

    const getTbProps = () => ({
        columns: [
            {
                title: <IntlText id={'NAME'}/>,
                dataIndex: 'name',
                key: 'name',
                ellipsis: true,
            },
            {
                title: <IntlText id={'TYPE'}/>,
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
        rowSelection: getRowSelection(),
        onRow,
        showHeader: true,
        scroll: height,
    });

    const getTbBtnProps = () => ([
        {
            title: intl.formatMessage({id: 'REFRESH_BTN'}),
            key: 'refresh',
            icon: <SyncOutlined className={styles.toolButtonIcon}/>,
            onClick: query,
        },
        {
            title: intl.formatMessage({id: 'UPLOAD_NEW'}),
            key: 'upload',
            icon: <UploadOutlined className={styles.toolButtonIcon}/>,
            onClick: () => setVisible(true),
        },
        {
            title: intl.formatMessage({id: 'DELETE'}),
            key: 'delete',
            icon: <DeleteIcon className={styles.toolButtonRedIcon}/>,
            onClick: removePlugin,
        },
        {
            title: 'Open new window',
            key: 'open',
            icon: <ArrowRightOutlined className={styles.toolButtonIcon}/>,
            onClick: () => {
                if (url) {
                    window.open(url, selected.rows[0].id);
                }
            },
        }
    ]);

    const formData = () => ({
        type: form.getFieldValue("type"),
    });

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
            return `/jarboot/plugins/page/${plugin.type}/${plugin.fileName}/index.html`;
        } else {
            return null;
        }
    };

    let tableOption: any = getTbProps();
    tableOption.scroll = { y: height};
    const url = getUrl();
    return (
        <Layout>
            <Sider width={300}>
                <CommonTable option={tableOption}
                             toolbar={getTbBtnProps()} height={height}/>
            </Sider>
            <Layout>
                {url && <iframe frameBorder={0} width={'100%'} height={'100%'} src={url}/>}
            </Layout>
            {visible && <Modal title={intl.formatMessage({id: 'UPLOAD_TITLE'})}
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
                    <Form.Item label={intl.formatMessage({id: 'FILE'})} name={"file"}>
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
