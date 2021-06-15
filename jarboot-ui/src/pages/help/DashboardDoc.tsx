import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";

const { Title, Paragraph, Text } = Typography;

const DashboardDoc: any = memo(() => {
    const intl = useIntl();
    return <>
        <Typography>
            <Title>dashboard</Title>
            <Paragraph>
                <Text>{intl.formatMessage({id: 'DASHBOARD_DESC'})}</Text>
                <Title level={2}>{intl.formatMessage({id: 'USAGE_DEMO'})}</Title>
                <pre>
                    USAGE:<br/>
                    dashboard [i] [n]<br/>
                </pre>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default DashboardDoc
