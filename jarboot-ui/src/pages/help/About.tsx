import React, {memo, useState} from "react";
import { Row, Col, Menu } from 'antd';
import {RocketOutlined, SettingOutlined} from '@ant-design/icons';
import {useIntl} from "umi";
import QuickStartDoc from "@/pages/help/QuickStartDoc";
import styles from "@/pages/index.less";
import SettingDoc from "@/pages/help/SettingDoc";
import CmdListDoc from "@/pages/help/CmdListDoc";

const pageMap: any = {
    'quick-start': <QuickStartDoc/>,
    'setting': <SettingDoc/>,
    'cmdList': <CmdListDoc/>,
};

const About: any = memo(() => {
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
                <Menu.ItemGroup title={intl.formatMessage({id: 'ADVANCED'})}>
                    <Menu.Divider/>
                    <Menu.Item key="cmdList">{intl.formatMessage({id: 'COMMAND_LIST'})}</Menu.Item>
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

export default About;
