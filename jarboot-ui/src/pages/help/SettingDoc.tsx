import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";

const { Title, Paragraph, Text } = Typography;

const SettingDoc: any = memo(() => {
    const intl = useIntl();
    return <>
        <Typography>
            <Title>{intl.formatMessage({id: 'SETTING'})}</Title>
            <Paragraph>
                <Title level={2}>{intl.formatMessage({id: 'SETTING_P1'})}</Title>
                <ul>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'SERVERS_PATH'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P2'})}
                    </li>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'DEFAULT_VM_OPT'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P3'})}
                    </li>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'MAX_START_TIME'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P4'})}
                    </li>
                </ul>
            </Paragraph>
            <Paragraph>
                <Title level={2}>{intl.formatMessage({id: 'SERVICES_CONF'})}</Title>
                <ul>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'JAR_LABEL'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P6'})}
                    </li>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'JVM_OPT_LABEL'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P7'})}
                    </li>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'MAIN_ARGS_LABEL'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P8'})}
                    </li>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'PRIORITY_LABEL'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P9'})}
                    </li>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'DAEMON_LABEL'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P10'})}
                    </li>
                    <li>
                        <Text keyboard>{intl.formatMessage({id: 'JAR_UPDATE_WATCH_LABEL'})}</Text>
                        {intl.formatMessage({id: 'SETTING_P11'})}
                    </li>
                </ul>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default SettingDoc
