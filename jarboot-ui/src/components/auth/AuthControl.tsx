import { Col, Row, Menu } from 'antd';
import {memo, useState} from "react";
import { useIntl } from 'umi';
import UserList from "@/components/auth/UserList";
import RoleMgr from "@/components/auth/RoleMgr";

const AuthControl = memo(() => {
    const intl = useIntl();
    let [selected, setSelected] = useState("UserList");
    const onSelect = (event: any) => {
        setSelected(event.key);
    };
    return <Row>
        <Col span={6}>
            <Menu selectedKeys={[selected]} mode="inline" onClick={onSelect}>
                <Menu.ItemGroup title={<span>
                    <span>{intl.formatMessage({id: 'AUTH_CONTROL'})}</span>
                </span>}>
                    <Menu.Divider/>
                    <Menu.Item key={'UserList'}>{intl.formatMessage({id: 'USER_LIST'})}</Menu.Item>
                    <Menu.Item key={'RoleMgr'}>{intl.formatMessage({id: 'ROLE_MGR'})}</Menu.Item>
                    <Menu.Item key={'PrivilegeMgr'}>{intl.formatMessage({id: 'PRIVILEGE_MGR'})}</Menu.Item>
                </Menu.ItemGroup>
            </Menu>
        </Col>
        <Col span={18}>
            {'UserList' === selected && <UserList/>}
            {'RoleMgr' === selected && <RoleMgr/>}
            {'PrivilegeMgr' === selected && "Designing and coding..."}
        </Col>
    </Row>
});
export default AuthControl;
