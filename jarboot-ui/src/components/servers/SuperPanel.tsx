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
import {JarBootConst} from "@/common/JarBootConst";
import {ServerPubsubImpl} from "@/components/servers/ServerPubsubImpl";

/**
 * 服务的多功能面板，控制台输出、命令执行结果渲染
 * @author majianzheng
 */

interface SuperPanelProps {
    server: string;
    sid: string;
    visible: boolean;
}

enum PUB_TOPIC {
    CMD_END="commandEnd",
    RENDER_JSON = "renderJson",
    QUICK_EXEC_CMD = "quickExecCmd",
    RECONNECTED = "reconnected",
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
const pubsub: PublishSubmit = new ServerPubsubImpl();

const SuperPanel = memo((props: SuperPanelProps) => { //NOSONAR
    const intl = useIntl();
    const [view, setView] = useState('');
    const [executing, setExecuting] = useState(false);
    const [command, setCommand] = useState("");
    const [data, setData] = useState({});
    const inputRef = useRef<any>();
    const key = props.sid;

    useEffect(() => {
        pubsub.submit(key, PUB_TOPIC.CMD_END, onCmdEnd);
        pubsub.submit(key, PUB_TOPIC.RENDER_JSON, renderView);
        pubsub.submit(key, PUB_TOPIC.QUICK_EXEC_CMD, onExecQuickCmd);
        historyMap.set(key, new class implements HistoryProp {
            cur = 0;
            history = [];
        });
        return () => {
            pubsub.unSubmit(key, PUB_TOPIC.CMD_END, onCmdEnd);
            pubsub.unSubmit(key, PUB_TOPIC.RENDER_JSON, renderView);
            pubsub.unSubmit(key, PUB_TOPIC.QUICK_EXEC_CMD, onExecQuickCmd);
        };
    }, []);

    const historyProp = historyMap.get(key);

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
        if (inputRef?.current?.props?.disabled) {
            CommonNotice.info(intl.formatMessage({id: 'COMMAND_RUNNING'}, {command: inputRef.current.state?.value}));
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
        pubsub.publish(key, JarBootConst.FINISH_LOADING, msg);
        inputRef?.current?.focus();
        const value = inputRef?.current?.state?.value;
        if (value && value?.length > 0) {
            inputRef.current.setSelectionRange(0, value.length);
        }
    };

    const clearDisplay = () => {
        pubsub.publish(key, JarBootConst.CLEAR_CONSOLE);
        inputRef?.current?.focus();
    };

    const closeView = () => {
        if (executing) {
            CommonNotice.info(intl.formatMessage({id: 'COMMAND_RUNNING'}, {command}));
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
        pubsub.publish(key, JarBootConst.APPEND_LINE, `jarboot$ ${cmd}`);
        const msg = {server: props.server, sid: props.sid, body: cmd, func: 1};
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
        const msg = {server: props.server, sid: props.sid, body: '', func: 2};
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
               placeholder={intl.formatMessage({id: 'COMMAND_PLACEHOLDER'})}
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
                <Console id={key}
                         visible={'' === view}
                         pubsub={pubsub}/>
                {'' !== view && viewResolver[view]}
            </div>
        </Card>
    </>
});

export {SuperPanel, PUB_TOPIC, pubsub};
