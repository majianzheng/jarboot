import { Layout, Menu } from "antd";
import styles from "@/common/global.less";
import React, {useState} from "react";
import {useIntl} from "umi";
import SystemSetting from "@/components/setting/SystemSetting";
import PluginsManager from "@/components/plugins";
import {PluginIcon} from "@/components/icons";
import {SettingOutlined, FileProtectOutlined} from "@ant-design/icons";
import TrustedHosts from "@/components/setting/TrustedHosts";

const Setting = () => {
    const intl = useIntl();
    const [page, setPage] = useState("SYSTEM_SETTING");
    const handleClick = (e: any) => {
        setPage(e.key);
    };

    const menus = [
        {
            id: 'SYSTEM_SETTING',
            icon: <SettingOutlined />,
            content: <div style={{padding: '0 5px 0 20px'}}><SystemSetting/></div>
        },
        {
            id: 'PLUGINS',
            icon: <PluginIcon/>,
            content: <PluginsManager/>
        },
        {
            id: 'TRUSTED_HOSTS',
            icon: <FileProtectOutlined/>,
            content: <TrustedHosts/>
        }
    ];

    return <Layout>
        <Layout.Sider width={280} className={styles.pageContainer} theme={"light"}>
            <Menu onClick={handleClick} defaultSelectedKeys={['SYSTEM_SETTING']} mode="inline">
                <Menu.ItemGroup title={intl.formatMessage({id: 'SETTING'})}>
                    <Menu.Divider/>
                    {
                        menus.map(menu => (
                            <Menu.Item key={menu.id} icon={menu.icon}>
                                {intl.formatMessage({id: menu.id})}
                            </Menu.Item>
                        ))
                    }
                </Menu.ItemGroup>
            </Menu>
        </Layout.Sider>
        <Layout.Content className={styles.pageContainer}>
            {menus.find(menu => page === menu.id)?.content}
        </Layout.Content>
    </Layout>;
};

export default Setting;
