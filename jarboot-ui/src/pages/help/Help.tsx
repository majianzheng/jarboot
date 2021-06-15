import React, {memo, useState} from "react";
import { Row, Col, Menu } from 'antd';
import {RocketOutlined, SettingOutlined} from '@ant-design/icons';
import {useIntl} from "umi";
import QuickStartDoc from "@/pages/help/QuickStartDoc";
import styles from "@/pages/index.less";
import SettingDoc from "@/pages/help/SettingDoc";
import BytesDoc from "@/pages/help/BytesDoc";
import JvmDoc from "@/pages/help/JvmDoc";
import JadDoc from "@/pages/help/JadDoc";
import DashboardDoc from "@/pages/help/DashboardDoc";
import GoGithubDoc from "@/pages/help/GoGithubDoc";

const pageMap: any = {
    'quick-start': <QuickStartDoc/>,
    'setting': <SettingDoc/>,
    'bytes': <BytesDoc/>,
    'jvm': <JvmDoc/>,
    'jad': <JadDoc/>,
    'dashboard': <DashboardDoc/>,
    'watch': <GoGithubDoc/>,
    'trace': <GoGithubDoc/>,
    'thread': <GoGithubDoc/>,
    'sysprop': <GoGithubDoc/>,
};

const Help: any = memo(() => {
    const intl = useIntl();
    const [page, setPage] = useState("quick-start");
    const handleClick = (e: any) => {
        console.log(e);
        setPage(e.key);
    };
    return <Row>
        <Col span={4} className={styles.pageContainer}>
            <Menu
                onClick={handleClick}
                defaultSelectedKeys={['quick-start']}
                mode="inline"
            >
                <Menu.ItemGroup title={intl.formatMessage({id: 'BASIC'})}>
                    <Menu.Divider/>
                    <Menu.Item key="quick-start">
                        <RocketOutlined />
                        {intl.formatMessage({id: 'QUICK_START'})}
                    </Menu.Item>
                    <Menu.Item key="setting">
                        <SettingOutlined />
                        {intl.formatMessage({id: 'SETTING'})}
                    </Menu.Item>
                </Menu.ItemGroup>
                <Menu.ItemGroup title={intl.formatMessage({id: 'COMMAND_LIST'})}>
                    <Menu.Divider/>
                    <Menu.Item key="bytes">bytes</Menu.Item>
                    <Menu.Item key="dashboard">dashboard</Menu.Item>
                    <Menu.Item key="jad">jad</Menu.Item>
                    <Menu.Item key="jvm">jvm</Menu.Item>
                    <Menu.Item key="watch">watch</Menu.Item>
                    <Menu.Item key="trace">trace</Menu.Item>
                    <Menu.Item key="thread">thread</Menu.Item>
                    <Menu.Item key="sysprop">sysprop</Menu.Item>
                </Menu.ItemGroup>

            </Menu>
        </Col>
        <Col span={20}>
            <div style={{padding: '0 5px 0 10px'}} className={styles.pageContainer}>
                {pageMap[page]}
            </div>
        </Col>
    </Row>
});

export default Help;
