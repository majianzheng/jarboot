import { Layout, Menu } from 'antd';
import React, {memo, useState} from "react";
import { useIntl } from 'umi';
import UserList from "@/components/auth/UserList";
import RoleMgr from "@/components/auth/RoleMgr";
import PrivilegeMgr from "@/components/auth/PrivilegeMgr";
import {UserOutlined, TeamOutlined} from "@ant-design/icons";
import {PrivilegeIcon} from "@/components/icons";
import styles from "@/common/global.less";

/**
 * 权限控制
 * @author majianzheng
 */
const AuthControl = memo(() => {
    const intl = useIntl();
    let [selected, setSelected] = useState("UserList");
    const onSelect = (event: any) => {
        setSelected(event.key);
    };
    return <Layout>
        <Layout.Sider width={280} theme={"light"}>
            <Menu selectedKeys={[selected]} mode="inline" onClick={onSelect}>
                <Menu.ItemGroup title={<span>
                    <span>{intl.formatMessage({id: 'AUTH_CONTROL'})}</span>
                </span>}>
                    <Menu.Divider/>
                    <Menu.Item key={'UserList'} icon={<UserOutlined />}>
                        {intl.formatMessage({id: 'USER_LIST'})}
                    </Menu.Item>
                    <Menu.Item key={'RoleMgr'} icon={<TeamOutlined />}>
                        {intl.formatMessage({id: 'ROLE_MGR'})}
                    </Menu.Item>
                    <Menu.Item key={'PrivilegeMgr'} icon={<PrivilegeIcon/>}>
                        {intl.formatMessage({id: 'PRIVILEGE_MGR'})}
                    </Menu.Item>
                </Menu.ItemGroup>
            </Menu>
        </Layout.Sider>
        <Layout.Content className={styles.pageContainer}>
            {'UserList' === selected && <UserList/>}
            {'RoleMgr' === selected && <RoleMgr/>}
            {'PrivilegeMgr' === selected && <PrivilegeMgr/>}
        </Layout.Content>
    </Layout>;
});
export default AuthControl;
