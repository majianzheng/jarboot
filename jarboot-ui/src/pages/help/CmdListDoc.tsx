import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";

const { Title, Paragraph, Text } = Typography;

const CmdListDoc: any = memo(() => {
    const intl = useIntl();
    return <>
        <Typography>
            <Title>{intl.formatMessage({id: 'CMD_LIST_DESC'})}</Title>
            <Paragraph>
                <Title level={2}>{intl.formatMessage({id: 'CMD_LIST_HELP'})}</Title>
                <ul>
                    <li><Text keyboard>bytes</Text></li>
                    <li><Text keyboard>stdout</Text></li>
                    <li><Text keyboard>dashboard</Text></li>
                    <li><Text keyboard>classloader</Text></li>
                    <li><Text keyboard>jad</Text></li>
                    <li><Text keyboard>sc</Text></li>
                    <li><Text keyboard>jvm</Text></li>
                    <li><Text keyboard>thread</Text></li>
                    <li><Text keyboard>watch</Text></li>
                    <li><Text keyboard>trace</Text></li>
                    <li><Text keyboard>sysprop</Text></li>
                </ul>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default CmdListDoc
