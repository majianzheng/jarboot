import styles from "./index.less";
import Console from "@/components/console";
import React, {KeyboardEvent, memo, useEffect, useRef, useState} from "react";
import {Button, Input} from "antd";
import {EnterOutlined, LoadingOutlined, ClearOutlined, CloseOutlined, RightOutlined} from "@ant-design/icons";
import StringUtil from "@/common/StringUtil";
import CommonNotice from "@/common/CommonNotice";
import {WsManager} from "@/common/WsManager";
import DashboardView from "@/components/servers/view/DashboardView";
import JadView from "@/components/servers/view/JadView";
import HeapDumpView from "@/components/servers/view/HeapDumpView";
import {useIntl} from "umi";
import {JarBootConst} from "@/common/JarBootConst";
import {PUB_TOPIC, pubsub} from "@/components/servers";
import TopTitleBar from "@/components/servers/TopTitleBar";

/**
 * 服务的多功能面板，控制台输出、命令执行结果渲染
 * @author majianzheng
 */

interface SuperPanelProps {
    server: string;
    sid: string;
    visible: boolean;
    remote?: string;
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

const MAX_HISTORY = 100;
const historyMap = new Map<string, HistoryProp>();

const SuperPanel = memo((props: SuperPanelProps) => {
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
        pubsub.submit(key, PUB_TOPIC.FOCUS_CMD_INPUT, onFocusCommandInput);
        historyMap.set(key, new class implements HistoryProp {
            cur = 0;
            history = [];
        });
        return () => {
            pubsub.unSubmit(key, PUB_TOPIC.CMD_END, onCmdEnd);
            pubsub.unSubmit(key, PUB_TOPIC.RENDER_JSON, renderView);
            pubsub.unSubmit(key, PUB_TOPIC.QUICK_EXEC_CMD, onExecQuickCmd);
            pubsub.unSubmit(key, PUB_TOPIC.FOCUS_CMD_INPUT, onFocusCommandInput);
        };
    }, []);

    const historyProp = historyMap.get(key);

    //解析json数据的视图
    const viewResolver = () => {
        let panel;
        switch (view) {
            case 'dashboard':
                panel = <DashboardView data={data}/>;
                break;
            case 'jad':
                panel = <JadView data={data}/>;
                break;
            case 'heapdump':
                panel = <HeapDumpView data={data} remote={props.remote}/>;
                break;
            default:
                panel = <div>Unknown command view {view}</div>;
                break;
        }
        const buttonTitle = executing ? intl.formatMessage({id: 'CANCEL'}) : intl.formatMessage({id: 'CLOSE'});
        return (<div style={{height: JarBootConst.PANEL_HEIGHT}}>
            <TopTitleBar title={command}
                         icon={executing && <LoadingOutlined className={styles.statusStarting}/>}
                         onClose={closeView}
                         closeButtonTitle={buttonTitle}/>
            {panel}
        </div>);
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

    const onFocusCommandInput = () => {
        inputRef?.current?.focus();
        const value = inputRef?.current?.state?.value;
        if (value && value?.length > 0) {
            inputRef.current.setSelectionRange(0, value.length);
        }
    };

    useEffect(onFocusCommandInput, [props.visible]);

    const onCmdEnd = (msg?: string) => {
        setExecuting(false);
        pubsub.publish(key, JarBootConst.FINISH_LOADING, msg);
        onFocusCommandInput();
    };

    const clearDisplay = () => {
        pubsub.publish(key, JarBootConst.CLEAR_CONSOLE);
        inputRef?.current?.focus();
    };

    const closeView = () => {
        if (executing) {
            onCancelCommand();
            return;
        }
        if ('' !== view) {
            //切换为控制台显示
            setView('');
        }
        Promise.resolve().then(onFocusCommandInput);
    };

    const doExecCommand = (cmd: string) => {
        if (StringUtil.isEmpty(cmd)) {
            return;
        }
        if (StringUtil.isEmpty(props.sid)) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_ONE_SERVER_INFO'}));
            return;
        }

        setExecuting(true);
        if ('' !== view) {
            //切换为控制台显示
            setView('');
        }
        pubsub.publish(key, JarBootConst.APPEND_LINE, `<span class="${styles.commandPrefix}">$</span>${cmd}`);
        WsManager.sendMessage({server: props.server, sid: props.sid, body: cmd, func: 1});

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
        if (StringUtil.isEmpty(props.sid)) {
            CommonNotice.info(intl.formatMessage({id: 'SELECT_ONE_SERVER_INFO'}));
            return;
        }
        const msg = {server: props.server, sid: props.sid, body: '', func: 2};
        WsManager.sendMessage(msg);
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

    const consolePanel = () => {
        const style = '' === view ? {height: JarBootConst.PANEL_HEIGHT} : {display: 'none'};
        return (<div style={style}>
            <Console id={key}
                     pubsub={pubsub}
                     height={JarBootConst.PANEL_HEIGHT - 26}
                     wrap={false}
                     autoScrollEnd={true}/>
            <Input onPressEnter={onExecCommand}
                   onKeyUp={onKeyUp}
                   ref={inputRef}
                   className={styles.commandInput}
                   disabled={executing}
                   placeholder={intl.formatMessage({id: 'COMMAND_PLACEHOLDER'})}
                   autoComplete={"off"}
                   autoCorrect="off"
                   autoCapitalize="off"
                   spellCheck="false"
                   onChange={event => setCommand(event.target.value)}
                   value={command}
                   prefix={<RightOutlined className={styles.commandRightIcon}/>}
                   suffix={executing ? <LoadingOutlined/> : <EnterOutlined onClick={onExecCommand}/>}
            />
            {'' === view && extraButton()}
        </div>);
    };

    const extraButton = () => {
        let extra;
        if (executing) {
            extra = <Button icon={<CloseOutlined />}
                            size={"small"}
                            ghost danger
                            onClick={onCancelCommand}/>;
        } else {
            extra = <Button icon={<ClearOutlined />}
                            size={"small"}
                            ghost danger
                            onClick={clearDisplay}/>;
        }
        return (<div className={styles.consoleExtra}>{extra}</div>);
    };
    return (
        <div style={{display: props.visible ? 'block' : 'none'}}>
            {consolePanel()}
            {'' !== view && viewResolver()}
            {JarBootConst.IS_SAFARI && '' === view && <div className={styles.consoleScrollbarMaskForMac}/>}
        </div>);
});

export {SuperPanel};
