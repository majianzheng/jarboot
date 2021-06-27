import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";

const { Title, Paragraph, Text } = Typography;

const PropertyFileDoc: any = memo(() => {
    const intl = useIntl();
    return <>
        <Typography>
            <Title>{intl.formatMessage({id: 'PROP_FILE_DESC'})}</Title>
            <Paragraph>
                <Text></Text>
                <pre>
#默认的端口是9899，可通过修改此配置项更改端口<br/>
#port=9899<br/>
<br/>
#文件更新抖动时间，单位秒(s)，范围[3, 600]，不在范围则默认5<br/>
#jarboot.file-shake-time=5<br/>
<br/>
#启动判定时间，进程多久没有控制台输出后判定为启动成功，单位毫秒(ms)，范围[1500, 30000]，默认5000<br/>
#jarboot.start-wait-time=5000<br/>
                </pre>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default PropertyFileDoc
