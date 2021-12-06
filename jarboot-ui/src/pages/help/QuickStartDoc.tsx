import React, {memo} from "react";
import {BackTop, Breadcrumb, Typography} from "antd";
import {useIntl} from "umi";
import {CaretRightOutlined, HomeOutlined, PoweroffOutlined, SyncOutlined} from "@ant-design/icons";
import {RestartIcon} from "@/components/icons";

const { Title, Paragraph, Text } = Typography;

const QuickStartDoc = memo(() => {
    const intl = useIntl();
    return <>
        <Breadcrumb>
            <Breadcrumb.Item><HomeOutlined /></Breadcrumb.Item>
            <Breadcrumb.Item>
                {intl.formatMessage({id: 'HELP'})}
            </Breadcrumb.Item>
            <Breadcrumb.Item>
                {intl.formatMessage({id: 'QUICK_START'})}
            </Breadcrumb.Item>
        </Breadcrumb>
        <Typography>
            <Title>{intl.formatMessage({id: 'QUICK_START'})}</Title>
            <Paragraph>
                {intl.formatMessage({id: 'QUICK_START_P2'}, {dir: <Text code>services</Text>})}
            </Paragraph>
            <Paragraph>
                {intl.formatMessage({id: 'QUICK_START_P3'})}
                <Text code>jarboot</Text>/<Text code>services</Text>/<Text code>your-service-name</Text>/<Text code>***.jar</Text>
            </Paragraph>
            <Paragraph>
                {intl.formatMessage({id: 'QUICK_START_P4'},
                    {
                        key1: <Text mark>「{intl.formatMessage({id: 'SETTING'})}」</Text>,
                        key2: <Text keyboard>{intl.formatMessage({id: 'SERVERS_PATH'})}</Text>,
                        key3: <Text mark>「{intl.formatMessage({id: 'SERVICES_CONF'})}」</Text>,
                        key4: <Text keyboard>{intl.formatMessage({id: 'COMMAND_LABEL'})}</Text>
                    })}
            </Paragraph>
            <Paragraph>
                {intl.formatMessage({id: 'QUICK_START_P5'},
                    {key: <Text mark>「{intl.formatMessage({id: 'SERVICES_MGR'})}」</Text>})}
                <ul>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'ONE_KEY_START'})}</Text>
                        <Text keyboard>{intl.formatMessage({id: 'ONE_KEY_STOP'})}</Text>
                        <Text keyboard>{intl.formatMessage({id: 'ONE_KEY_RESTART'})}</Text>
                        {intl.formatMessage({id: 'QUICK_START_P6'})}
                    </li>
                    <li>
                        <Text keyboard><CaretRightOutlined style={{color: '#1DA57A'}}/></Text>
                        <Text keyboard><PoweroffOutlined style={{color: 'red'}}/></Text>
                        <Text keyboard><RestartIcon style={{color: '#1DA57A'}}/></Text>
                        {intl.formatMessage({id: 'QUICK_START_P7'})}
                    </li>
                    <li>
                        <Text keyboard><SyncOutlined style={{color: '#1DA57A'}}/></Text>
                        {intl.formatMessage({id: 'QUICK_START_P8'})}
                    </li>
                </ul>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default QuickStartDoc
