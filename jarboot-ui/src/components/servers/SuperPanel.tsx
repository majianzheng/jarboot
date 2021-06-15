import * as React from "react";
import styles from "./index.less";
import Console from "@/components/console/Console";
import {memo, useEffect, useRef, useState} from "react";
import {Button, Card, Input} from "antd";
import {formatMsg} from "@/common/IntlFormat";
import {CloseCircleOutlined, EnterOutlined, LoadingOutlined} from "@ant-design/icons";
import StringUtil from "@/common/StringUtil";
import CommonNotice from "@/common/CommonNotice";
import {WsManager} from "@/common/WsManager";
import DashboardView from "@/components/servers/view/DashboardView";
import JadView from "@/components/servers/view/JadView";

/**
 * 服务的多功能面板，控制台输出、命令执行结果渲染
 * @author majianzheng
 */

interface SuperPanelProps {
    server: string;
    visible: boolean;
    pubsub: PublishSubmit;
}

enum PUB_TOPIC {
    CMD_OVER="commandOver",
    RENDER_JSON = "renderJson"
}

const outHeight = `${window.innerHeight - 150}px`;

const SuperPanel = memo((props: SuperPanelProps) => {

    const [view, setView] = useState('');
    const [executing, setExecuting] = useState(false);
    const [command, setCommand] = useState("");
    const [data, setData] = useState({});
    const inputRef = useRef<any>();

    useEffect(() => {
        props.pubsub.submit(props.server, PUB_TOPIC.CMD_OVER, onCmdOver);
        props.pubsub.submit(props.server, PUB_TOPIC.RENDER_JSON, renderView);
    });

    //解析json数据的视图
    const viewResolver: any = {
        'dashboard': <DashboardView data={data}/>,
        'jad': <JadView data={data}/>,
    };

    const renderView = (data: any) => {
        const cmd = data.name;
        if (cmd !== view) {
            setView(cmd);
        }
        setData(data);
    };

    const onCmdOver = () => {
        setExecuting(false);
        props.pubsub.publish(props.server, 'finishLoading');
        inputRef?.current?.focus();
    };

    const clearDisplay = () => {
        props.pubsub.publish(props.server, 'clear');
        inputRef?.current?.focus();
    };

    const onExecCommand = () => {
        if (StringUtil.isEmpty(command)) {
            return;
        }
        if (StringUtil.isEmpty(props.server)) {
            CommonNotice.info('请选择一个服务后操作');
            return;
        }

        setExecuting(true);
        if ('' !== view) {
            //切换为控制台显示
            setView('');
        }
        props.pubsub.publish(props.server, 'appendLine', `>${command}`);
        const msg = {server: props.server, body: command, func: 1};
        WsManager.sendMessage(JSON.stringify(msg));
    };

    const onCancelCommand = () => {
        if (StringUtil.isEmpty(props.server)) {
            CommonNotice.info('请选择一个服务后操作');
            return;
        }
        const msg = {server: props.server, body: '', func: 2};
        WsManager.sendMessage(JSON.stringify(msg));
    };

    const outTitle = (<>
        <Input onPressEnter={onExecCommand}
               ref={inputRef}
               disabled={executing}
               placeholder={"command..."}
               autoComplete={"off"}
               autoCorrect="off"
               autoCapitalize="off"
               spellCheck="false"
               style={{width: '100%'}}
               onChange={event => setCommand(event.target.value)}
               value={command}
               suffix={executing ? <LoadingOutlined/> : <span/>}
               addonAfter={executing ?
                   <CloseCircleOutlined onClick={onCancelCommand}/> :
                   <EnterOutlined onClick={onExecCommand}/>}/>
    </>);

    return <>
        <Card title={outTitle} size={"small"}
              style={{display: props.visible ? 'block' : 'none'}}
              extra={<Button type={"link"} onClick={clearDisplay}>{formatMsg('CLEAR')}</Button>}>
            <div className={styles.outPanel} style={{height: outHeight}}>
                <Console server={props.server}
                         visible={'' === view}
                         pubsub={props.pubsub}/>
                {'' !== view && viewResolver[view]}
            </div>
        </Card>
    </>
});

export {SuperPanel, PUB_TOPIC};
