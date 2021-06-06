import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";

const { Title, Paragraph, Text } = Typography;

const BytesDoc: any = memo(() => {
    const intl = useIntl();
    return <>
        <Typography>
            <Title>bytes</Title>
            <Paragraph>
                <Text>{intl.formatMessage({id: 'BYTES_DESC'})}</Text>
                <Title level={2}>{intl.formatMessage({id: 'USAGE_DEMO'})}</Title>
                <pre>
                    USAGE:<br/>
                    bytes [class name]<br/>
                    <br/>
                    SUMMARY:<br/>
                    Display the class bytes detail.<br/>
                    <br/>
                    EXAMPLES:<br/>
                    bytes java.lang.String<br/>
                </pre>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default BytesDoc
