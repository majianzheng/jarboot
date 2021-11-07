import React, {memo, useState} from "react";
import {Row, Col, Menu, Typography} from 'antd';
import {RocketOutlined, SettingOutlined} from '@ant-design/icons';
import {useIntl} from "umi";
import QuickStartDoc from "@/pages/help/QuickStartDoc";
import styles from "@/pages/index.less";
import SettingDoc from "@/pages/help/SettingDoc";

const { Title, Paragraph, Link } = Typography;

const About = memo(() => {
    const intl = useIntl();
    const [page, setPage] = useState("quick-start");

    const handleClick = (e: any) => {
        setPage(e.key);
    };

    return <Row>
        <Col span={4} className={styles.pageContainer}>
            <Menu onClick={handleClick} defaultSelectedKeys={['quick-start']} mode="inline">
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
            </Menu>
        </Col>
        <Col span={20}>
            <div style={{padding: '0 5px 0 10px'}} className={styles.pageContainer}>
                {"quick-start" === page && <Typography>
                    <Title>{intl.formatMessage({id: 'HELP_DOC'})}</Title>
                    <Paragraph>
                        {intl.formatMessage({id: 'ABOUT_TEXT'})}
                    </Paragraph>
                    <Paragraph>GitHub: <Link>https://github.com/majianzheng/jarboot</Link></Paragraph>
                    <Paragraph>Gitee: <Link>https://gitee.com/majz0908/jarboot</Link></Paragraph>
                    <Paragraph>Docker Hub: <Link>https://registry.hub.docker.com/r/mazheng0908/jarboot</Link></Paragraph>
                </Typography>}
                {"quick-start" === page && <QuickStartDoc/>}
                {"setting" === page && <SettingDoc/>}
            </div>
        </Col>
    </Row>
});

export default About;
