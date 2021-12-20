import {memo, default as React, useEffect, useState} from "react";
import { useIntl } from 'umi';
import CommonTable from "@/components/table";
import {JarBootConst} from "@/common/JarBootConst";
import {FormOutlined, SyncOutlined, UserAddOutlined, ExclamationCircleOutlined} from "@ant-design/icons";
import UserService from "@/services/UserService";
import CommonNotice from "@/common/CommonNotice";
import ModifyUserModal from "@/components/extra/ModifyUserModal";
import {Modal} from "antd";
import {DeleteIcon} from "@/components/icons";
import styles from "@/common/global.less";
import IntlText from "@/common/IntlText";

let isCreate = true;
let username = '';

/**
 * 用户管理
 * @author majianzheng
 */
const UserList = memo(() => {
    const intl = useIntl();
    let [loading, setLoading] = useState(false);
    let [selected, setSelected] = useState({
        selectedRowKeys: new Array<any>(), selectedRows: new Array<any>()
    });
    let [visible, setVisible] = useState(false);
    let [data, setData] = useState(new Array<any>());

    useEffect(() => query(), []);

    const query = () => {
        setLoading(true);
        UserService.getUsers(0, 10000000).then(resp => {
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
                title: <IntlText id={'NAME'}/>,
                dataIndex: 'username',
                key: 'username',
                ellipsis: true,
            },
            {
                title: <IntlText id={'PASSWORD'}/>,
                dataIndex: 'password',
                key: 'password',
                ellipsis: true,
                render: () => '*************************************'
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

    const onCreate = () => {
        isCreate = true;
        username = '';
        setVisible(true);
    };

    const onModify = () => {
        if (selected?.selectedRows && selected.selectedRows.length !== 1) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_ONE_OP'}));
            return;
        }
        isCreate = false;
        username = selected.selectedRows[0]?.username;
        setVisible(true);
    };

    const onDelete = () => {
        if (selected?.selectedRows && selected.selectedRows.length !== 1) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_ONE_OP'}));
            return;
        }
        const user = selected.selectedRows[0];
        if (JarBootConst.currentUser.username === user.username) {
            //不可删除自己
            CommonNotice.info(intl.formatMessage({id: 'CAN_NOT_REMOVE_SELF'}));
            return;
        }
        Modal.confirm({
            title: intl.formatMessage({id: 'DELETE'}),
            icon: <ExclamationCircleOutlined />,
            content: intl.formatMessage({id: 'DELETE_USER'}, {user: user.username}),
            onOk() {
                UserService.deleteUser(user.id).then(resp => {
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

    const _getTbBtnProps = () => {
        return [
            {
                title: intl.formatMessage({id: 'CREATE'}),
                key: 'add ',
                icon: <UserAddOutlined className={styles.toolButtonIcon}/>,
                onClick: onCreate,
            },
            {
                title: intl.formatMessage({id: 'MODIFY'}),
                key: 'modify ',
                icon: <FormOutlined className={styles.toolButtonIcon}/>,
                onClick: onModify,
            },
            {
                title: intl.formatMessage({id: 'DELETE'}),
                key: 'delete',
                icon: <DeleteIcon className={styles.toolButtonRedIcon}/>,
                onClick: onDelete,
            },
            {
                title: intl.formatMessage({id: 'REFRESH_BTN'}),
                key: 'refresh',
                icon: <SyncOutlined className={styles.toolButtonIcon}/>,
                onClick: query,
            },
        ]
    };

    const onClose = (success?: boolean) => {
        setVisible(false);
        isCreate && success && query();
    };

    tableOption.scroll = { y: JarBootConst.PANEL_HEIGHT};
    return <><CommonTable option={tableOption}
                          toolbar={_getTbBtnProps()} showToolbarName={true}
                          height={JarBootConst.PANEL_HEIGHT}/>
        {visible && <ModifyUserModal visible={true} isCreate={isCreate} username={username} onClose={onClose}/>}
    </>;
});

export default UserList;
