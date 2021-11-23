import React, {memo, useEffect, useState} from "react";
import { useIntl } from 'umi';
import CommonTable from "@/components/table";
import {JarBootConst} from "@/common/JarBootConst";
import {SyncOutlined, PlusSquareOutlined, ExclamationCircleOutlined} from "@ant-design/icons";
import CommonNotice from "@/common/CommonNotice";
import RoleService from "@/services/RoleService";
import {Form, Input, Modal} from "antd";
import {DeleteIcon} from "@/components/icons";
import styles from "@/common/global.less";

/**
 * 角色管理
 * @author majianzheng
 */
const RoleMgr = memo(() => {
    const intl = useIntl();
    const [form] = Form.useForm();
    let [loading, setLoading] = useState(false);
    let [visible, setVisible] = useState(false);
    let [selected, setSelected] = useState({
        selectedRowKeys: new Array<any>(), selectedRows: new Array<any>()
    });
    let [data, setData] = useState(new Array<any>());

    useEffect(() => query(), []);

    const query = () => {
        setLoading(true);
        RoleService.getRoles(0, 10000000).then(resp => {
            if (resp.resultCode !== 0) {
                CommonNotice.errorFormatted(resp);
                return;
            }
            setData(resp.result);
            setLoading(false);
        }).catch(CommonNotice.errorFormatted);
    };

    const _getRowSelection = () => {
        return {
            type: 'radio',
            onChange: (selectedRowKeys: any, selectedRows: any) => {
                setSelected({selectedRowKeys, selectedRows});
            },
            selectedRowKeys: selected.selectedRowKeys,
        };
    };

    const _onRowClick = (record: any) => {
        return {
            onClick: () => setSelected({selectedRowKeys: [record.id], selectedRows: [record]}),
        };
    };

    let tableOption: any = {
        columns: [
            {
                title: intl.formatMessage({id: 'ROLE'}),
                dataIndex: 'role',
                key: 'role',
                ellipsis: true,
            },
            {
                title: intl.formatMessage({id: 'NAME'}),
                dataIndex: 'username',
                key: 'username',
                ellipsis: true,
            },
        ],
        loading: loading,
        dataSource: data,
        pagination: false,
        rowKey: 'id',
        size: 'small',
        rowSelection: _getRowSelection(),
        onRow: _onRowClick,
        showHeader: true,
        scroll: JarBootConst.PANEL_HEIGHT,
    };

    const onBindClick = () => {
        setVisible(true);
    };

    const onDeleteClick = () => {
        if (selected?.selectedRows && selected.selectedRows.length !== 1) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_ONE_OP'}));
            return;
        }
        const roleInfo = selected.selectedRows[0];
        Modal.confirm({
            title: intl.formatMessage({id: 'DELETE'}),
            icon: <ExclamationCircleOutlined />,
            content: intl.formatMessage({id: 'DELETE_ROLE'}),
            onOk() {
                RoleService.deleteRole(roleInfo.role, roleInfo.username).then(resp => {
                    if (0 === resp.resultCode) {
                        CommonNotice.info(intl.formatMessage({id: 'SUCCESS'}));
                        query();
                    } else {
                        CommonNotice.errorFormatted(resp);
                    }
                }).catch(CommonNotice.errorFormatted);
            }
        });
    };

    const onModalClose = () => {
        setVisible(false);
    };

    const onOk = () => {
        form.submit();
    };

    const onSubmit = (formData: any) => {
        //提交
        RoleService.addRole(formData.role, formData.username).then(resp => {
            if (0 === resp.resultCode) {
                onModalClose();
                CommonNotice.info(intl.formatMessage({id: 'SUCCESS'}));
                query();
            } else {
                CommonNotice.errorFormatted(resp);
            }
        }).catch(CommonNotice.errorFormatted);
    };

    const _getTbBtnProps = () => {
        return [
            {
                name: intl.formatMessage({id: 'BIND_ROLE'}),
                key: 'banding ',
                icon: <PlusSquareOutlined className={styles.toolButtonIcon}/>,
                onClick: onBindClick,
            },
            {
                name: intl.formatMessage({id: 'DELETE'}),
                key: 'delete',
                icon: <DeleteIcon className={styles.toolButtonRedIcon}/>,
                onClick: onDeleteClick,
            },
            {
                name: intl.formatMessage({id: 'REFRESH_BTN'}),
                key: 'refresh',
                icon: <SyncOutlined className={styles.toolButtonIcon}/>,
                onClick: query,
            },
        ]
    };

    tableOption.scroll = { y: JarBootConst.PANEL_HEIGHT};
    const style = {height: '38px', fontSize: '16px', width: '100%'};
    return <>
        <CommonTable option={tableOption}
                     toolbar={_getTbBtnProps()} showToolbarName={true}
                     height={JarBootConst.PANEL_HEIGHT}/>
        {visible && <Modal title={intl.formatMessage({id: 'BIND_ROLE'})}
               visible={true}
               destroyOnClose={true}
               onOk={onOk}
               onCancel={onModalClose}>
            <Form form={form}
                  name="roleInfo-binding-form"
                  initialValues={{username: '', role: ''}}
                  onFinish={onSubmit}>
                <Form.Item name="role"
                           rules={[{ required: true, message: intl.formatMessage({id: 'INPUT_ROLE'}) }]}>
                    <Input autoComplete="off"
                           autoCorrect="off"
                           autoCapitalize="off"
                           spellCheck="false"
                           placeholder={intl.formatMessage({id: 'ROLE'})} style={style}/>
                </Form.Item>
                <Form.Item name="username"
                           rules={[{ required: true, message: intl.formatMessage({id: 'INPUT_USERNAME'})}]}>
                    <Input autoComplete="off"
                           autoCorrect="off"
                           autoCapitalize="off"
                           spellCheck="false"
                           placeholder={intl.formatMessage({id: 'USER_NAME'})} style={style}/>
                </Form.Item>
            </Form>
        </Modal>}
    </>;
});

export default RoleMgr;
