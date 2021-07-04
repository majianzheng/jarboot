import React, {memo, useState} from "react";
import {JarBootConst} from "@/common/JarBootConst";
import {Avatar, Menu, Popover} from "antd";
import {CaretDownOutlined, LogoutOutlined, FormOutlined, UserOutlined} from "@ant-design/icons";
import {useIntl} from "umi";
import ModifyUserModal from "@/components/extra/ModifyUserModal";
import CommonUtils from "@/common/CommonUtils";

const UserPopMenu: any = memo((props: any) => {
    const intl = useIntl();
    let [visible, setVisible] = useState(false);
    const SIGN_OUT_KEY = "sign-out";
    const MODIFY_PWD_KEY = "modify-password";
    const handleClick = (event: any) => {
        console.info(event);
        if (SIGN_OUT_KEY === event.key) {
            props.onHide && props.onHide();
            //用户注销
            localStorage.removeItem(JarBootConst.TOKEN_KEY);
            CommonUtils.loginPage();
        }
        if (MODIFY_PWD_KEY === event.key) {
            props.onHide && props.onHide();
            //用户修改密码
            setVisible(true);
        }
    };
    const onClose = () => {
        setVisible(false);
    };
    return <>
        <Menu onClick={handleClick} selectedKeys={[]}
              defaultSelectedKeys={['user-title']} mode="inline"
              style={{width: 220}}>
            <Menu.Item key={"user-title"}><UserOutlined/>{props?.username}</Menu.Item>
            <Menu.Divider/>
            <Menu.Item key={MODIFY_PWD_KEY}>
                <FormOutlined />
                {intl.formatMessage({id: 'MODIFY_PWD'})}
            </Menu.Item>
            <Menu.Item key={SIGN_OUT_KEY}>
                <LogoutOutlined />
                {intl.formatMessage({id: 'SIGN_OUT'})}
            </Menu.Item>
        </Menu>
        {visible && <ModifyUserModal username={JarBootConst.currentUser.username}
                                     onClose={onClose} visible={visible}/>}
    </>;
});

interface UserMenuProp {
    username: string;
    className?: any;
}
const UserMenu = (props: UserMenuProp) => {
    let [userMenuVisible, setUserMenuVisible] = useState(false);
    return <Popover content={<UserPopMenu username={props.username} onHide={() => setUserMenuVisible(false)}/>}
                    visible={userMenuVisible}
                    mouseLeaveDelay={0.5}
                    onVisibleChange={(visible) => setUserMenuVisible(visible)}
                    placement="bottomRight">
        <Avatar className={props.className} alt={props.username} icon={<UserOutlined/>}/>
        <span style={{verticalAlign: 'text-bottom'}}>{props.username}</span>
        <CaretDownOutlined style={{verticalAlign: 'text-top'}}/>
    </Popover>;
}
export default UserMenu;
