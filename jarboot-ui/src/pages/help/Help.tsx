import React, {memo, useState} from "react";
import {Layout, Menu, Typography, Breadcrumb} from 'antd';
import {RocketOutlined, SettingOutlined, StarOutlined, HomeOutlined} from '@ant-design/icons';
import {useIntl} from "umi";
import QuickStartDoc from "@/pages/help/QuickStartDoc";
import styles from "@/common/global.less";
import SettingDoc from "@/pages/help/SettingDoc";

const { Title, Paragraph, Link } = Typography;

const Help = memo(() => {
    const intl = useIntl();
    const [page, setPage] = useState("about");

    const handleClick = (e: any) => {
        setPage(e.key);
    };

    return <Layout>
        <Layout.Sider width={280} theme={"light"} className={styles.pageContainer}>
            <Menu onClick={handleClick} defaultSelectedKeys={['about']} mode="inline">
                <Menu.ItemGroup title={intl.formatMessage({id: 'HELP'})}>
                    <Menu.Divider/>
                    <Menu.Item key="about" icon={<StarOutlined />}>
                        {intl.formatMessage({id: 'ABOUT'})}
                    </Menu.Item>
                    <Menu.Item key="quick-start" icon={<RocketOutlined />}>
                        {intl.formatMessage({id: 'QUICK_START'})}
                    </Menu.Item>
                    <Menu.Item key="setting" icon={<SettingOutlined />}>
                        {intl.formatMessage({id: 'SETTING'})}
                    </Menu.Item>
                </Menu.ItemGroup>
            </Menu>
        </Layout.Sider>
        <Layout.Content>
            <div style={{padding: '0 5px 0 10px'}} className={styles.pageContainer}>
                {"about" === page && <div>
                    <Breadcrumb>
                        <Breadcrumb.Item><HomeOutlined /></Breadcrumb.Item>
                        <Breadcrumb.Item>
                            {intl.formatMessage({id: 'HELP'})}
                        </Breadcrumb.Item>
                        <Breadcrumb.Item>
                            {intl.formatMessage({id: 'ABOUT'})}
                        </Breadcrumb.Item>
                    </Breadcrumb>
                    <Typography>
                        <Title>{intl.formatMessage({id: 'ABOUT'})}</Title>
                        <Paragraph>
                            {intl.formatMessage({id: 'ABOUT_TEXT'})}
                        </Paragraph>
                        <Paragraph>GitHub: <Link>https://github.com/majianzheng/jarboot</Link></Paragraph>
                        <Paragraph>Gitee: <Link>https://gitee.com/majz0908/jarboot</Link></Paragraph>
                        <Paragraph>Docker
                            Hub: <Link>https://registry.hub.docker.com/r/mazheng0908/jarboot</Link></Paragraph>
                    </Typography>
                </div>}
                {"quick-start" === page && <QuickStartDoc/>}
                {"setting" === page && <SettingDoc/>}
            </div>
        </Layout.Content>
    </Layout>;
});

export default Help;
