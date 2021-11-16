import { Row, Col, Menu } from "antd";
import styles from "@/pages/index.less";
import React, {useState} from "react";
import {useIntl} from "umi";
import SystemSetting from "@/components/setting/SystemSetting";

const Setting = () => {
    const intl = useIntl();
    const [page, setPage] = useState("sys-setting");
    const handleClick = (e: any) => {
        setPage(e.key);
    };

    return <Row>
        <Col span={4} className={styles.pageContainer}>
            <Menu onClick={handleClick} defaultSelectedKeys={['sys-setting']} mode="inline">
                <Menu.ItemGroup title={intl.formatMessage({id: 'SETTING'})}>
                    <Menu.Divider/>
                    <Menu.Item key="sys-setting">
                        {intl.formatMessage({id: 'SYSTEM_SETTING'})}
                    </Menu.Item>
                </Menu.ItemGroup>
            </Menu>
        </Col>
        <Col span={20}>
            <div style={{padding: '0 5px 0 20px'}} className={styles.pageContainer}>
                {'sys-setting' === page && <SystemSetting/>}
            </div>
        </Col>
    </Row>
};

export default Setting;
