import styles from "./index.less";
import Console from "@/components/console";
import {KeyboardEvent, memo, useEffect, useRef, useState} from "react";
import {Button, Card, Input} from "antd";
import {CloseCircleOutlined, EnterOutlined, LoadingOutlined} from "@ant-design/icons";
import StringUtil from "@/common/StringUtil";
import CommonNotice from "@/common/CommonNotice";
import {WsManager} from "@/common/WsManager";
import DashboardView from "@/components/servers/view/DashboardView";
import JadView from "@/components/servers/view/JadView";
import HeapDumpView from "@/components/servers/view/HeapDumpView";
import {useIntl} from "umi";

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
    CMD_END="commandEnd",
    RENDER_JSON = "renderJson",
    QUICK_EXEC_CMD = "quickExecCmd",
}

/**
 * 执行记录，上下键
 */
interface HistoryProp {
    /** 当前的游标 */
    cur: number;
    /** 历史记录存储 */
    history: string[];
}

const outHeight = `${window.innerHeight - 150}px`;
const MAX_HISTORY = 100;
const historyMap = new Map<string, HistoryProp>();

const SuperPanel = memo((props: SuperPanelProps) => { //NOSONAR
    const intl = useIntl();
    const [view, setView] = useState('');
    const [executing, setExecuting] = useState(false);
    const [command, setCommand] = useState("");
    const [data, setData] = useState({});
    const inputRef = useRef<any>();

    useEffect(() => {
        props.pubsub.submit(props.server, PUB_TOPIC.CMD_END, onCmdEnd);
        props.pubsub.submit(props.server, PUB_TOPIC.RENDER_JSON, renderView);
        props.pubsub.submit(props.server, PUB_TOPIC.QUICK_EXEC_CMD, onExecQuickCmd);
        historyMap.set(props.server, new class implements HistoryProp {
            cur = 0;
            history = [];
        });
    }, []);

    const historyProp = historyMap.get(props.server);

    //解析json数据的视图
    const viewResolver: any = {
        'dashboard': <DashboardView data={data}/>,
        'jad': <JadView data={data}/>,
        'heapdump': <HeapDumpView data={data}/>,
    };

    const renderView = (resultData: any) => {
        const cmd = resultData.name;
        if (cmd !== view) {
            setView(cmd);
        }
        setData(resultData);
    };

    const onExecQuickCmd = (cmd: string) => {
        if (executing) {
            return;
        }
        if (StringUtil.isEmpty(cmd)) {
            return;
        }
        setCommand(cmd);
        doExecCommand(cmd);
    };

    const onCmdEnd = (msg?: string) => {
        setExecuting(false);
        props.pubsub.publish(props.server, 'finishLoading', msg);
        inputRef?.current?.focus();
        const value = inputRef?.current?.state?.value;
        if (value && value?.length > 0) {
            inputRef.current.setSelectionRange(0, value.length);
        }
    };

    const clearDisplay = () => {
        props.pubsub.publish(props.server, 'clear');
        inputRef?.current?.focus();
    };

    const closeView = () => {
        if (executing) {
            CommonNotice.info(intl.formatMessage({id: 'COMMAND_RUNNING'}));
            return;
        }
        if ('' !== view) {
            //切换为控制台显示
            setView('');
        }
        inputRef?.current?.focus();
    };

    const doExecCommand = (cmd: string) => {
        if (StringUtil.isEmpty(cmd)) {
            return;
        }
        if (StringUtil.isEmpty(props.server)) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_ONE_SERVER_INFO'}));
            return;
        }

        setExecuting(true);
        if ('' !== view) {
            //切换为控制台显示
            setView('');
        }
        props.pubsub.publish(props.server, 'appendLine', `jarboot$ ${cmd}`);
        const msg = {server: props.server, body: cmd, func: 1};
        WsManager.sendMessage(JSON.stringify(msg));

        if (historyProp) {
            const history = historyProp.history;
            if (history.length > 0 && history[history.length - 1] === cmd) {
                return;
            }
            history.push(cmd);
            if (history.length > MAX_HISTORY) {
                history.shift();
            }
            historyProp.cur = history.length - 1;
        }
    };

    const onExecCommand = () => {
        doExecCommand(command);
    };

    const onCancelCommand = () => {
        if (StringUtil.isEmpty(props.server)) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_ONE_SERVER_INFO'}));
            return;
        }
        const msg = {server: props.server, body: '', func: 2};
        WsManager.sendMessage(JSON.stringify(msg));
    };

    const onKeyUp = (e: KeyboardEvent) => {
        if ('ArrowUp' === e.key && historyProp) {
            const history = historyProp.history;
            historyProp.cur--;
            if (historyProp.cur < 0) {
                historyProp.cur = 0;
                return;
            }
            const value = history[historyProp.cur];
            if (value) {
                setCommand(value);
            }
            return;
        }
        if ('ArrowDown' === e.key && historyProp) {
            const history = historyProp.history;
            historyProp.cur++;
            if (historyProp.cur >= history.length) {
                historyProp.cur = history.length - 1;
                return;
            }
            const value = history[historyProp.cur];
            if (value) {
                setCommand(value);
            }
        }
    };

    const outTitle = (<>
        <Input onPressEnter={onExecCommand} onKeyUp={onKeyUp}
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

    const clearBtn = <Button type={"link"} onClick={clearDisplay}>{intl.formatMessage({id: 'CLEAR'})}</Button>;
    const closeBtn = <Button type={"link"} onClick={closeView}>{intl.formatMessage({id: 'CLOSE'})}</Button>;
    const extra = '' === view ? clearBtn : closeBtn;
    return <>
        <Card title={outTitle} size={"small"}
              style={{display: props.visible ? 'block' : 'none'}}
              extra={extra}>
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
