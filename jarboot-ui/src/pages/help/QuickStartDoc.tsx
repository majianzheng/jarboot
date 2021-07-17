import React, {memo} from "react";
import {BackTop, Typography} from "antd";
import {useIntl} from "umi";
import styles from "@/pages/index.less";
import {GithubOutlined, YuqueFilled, CaretRightOutlined, PoweroffOutlined, ReloadOutlined, SyncOutlined} from "@ant-design/icons";

const { Title, Paragraph, Text, Link } = Typography;

const QuickStartDoc: any = memo(() => {
    const intl = useIntl();
    return <>
        <Typography>
            <Title>{intl.formatMessage({id: 'QUICK_START'})}</Title>
            <Paragraph>
                <Text>{intl.formatMessage({id: 'MENU_DOCS'})}: </Text>
                <Link target={"_blank"}
                      href={"https://www.yuque.com/jarboot/usage/tmpomo"}>
                    <YuqueFilled style={{color: "green"}}/>
                    https://www.yuque.com/jarboot/usage/tmpomo
                </Link>
            </Paragraph>
            <Paragraph>
                {intl.formatMessage({id: 'QUICK_START_P1'}, {
                    github: <Link target={"_blank"}
                                  href={"https://github.com/majianzheng/jarboot"}
                    ><GithubOutlined className={styles.githubIcon}/>Github</Link>
                    })}
                <Link target={"_blank"}
                   href={"https://gitee.com/majz0908/jarboot"}>
                    Gitee
                </Link>
            </Paragraph>
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
                        key4: <Text keyboard>{intl.formatMessage({id: 'JAR_LABEL'})}</Text>
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
                        <Text keyboard><CaretRightOutlined style={{color: 'green'}}/></Text>
                        <Text keyboard><PoweroffOutlined style={{color: 'red'}}/></Text>
                        <Text keyboard><ReloadOutlined/></Text>
                        {intl.formatMessage({id: 'QUICK_START_P7'})}
                    </li>
                    <li>
                        <Text keyboard><SyncOutlined/></Text>
                        {intl.formatMessage({id: 'QUICK_START_P8'})}
                    </li>
                </ul>
            </Paragraph>
        </Typography>
        <BackTop/>
    </>;
});

export default QuickStartDoc
