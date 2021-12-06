import { Layout, Menu } from "antd";
import styles from "@/common/global.less";
import React, {useState} from "react";
import {useIntl} from "umi";
import SystemSetting from "@/components/setting/SystemSetting";
import PluginsManager from "@/components/plugins";
import {PluginIcon} from "@/components/icons";
import {SettingOutlined} from "@ant-design/icons";

const Setting = () => {
    const intl = useIntl();
    const [page, setPage] = useState("sys-setting");
    const handleClick = (e: any) => {
        setPage(e.key);
    };

    return <Layout>
        <Layout.Sider width={280} className={styles.pageContainer} theme={"light"}>
            <Menu onClick={handleClick} defaultSelectedKeys={['sys-setting']} mode="inline">
                <Menu.ItemGroup title={intl.formatMessage({id: 'SETTING'})}>
                    <Menu.Divider/>
                    <Menu.Item key="sys-setting" icon={<SettingOutlined />}>
                        {intl.formatMessage({id: 'SYSTEM_SETTING'})}
                    </Menu.Item>
                    <Menu.Item key="sys-plugins" icon={<PluginIcon/>}>
                        {intl.formatMessage({id: 'PLUGINS'})}
                    </Menu.Item>
                </Menu.ItemGroup>
            </Menu>
        </Layout.Sider>
        <Layout.Content className={styles.pageContainer}>
            {'sys-setting' === page && <div style={{padding: '0 5px 0 20px'}}>
                <SystemSetting/>
            </div>}
            {"sys-plugins" === page && <PluginsManager/>}
        </Layout.Content>
    </Layout>;
};

export default Setting;
