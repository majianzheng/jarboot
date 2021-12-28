import styles from "./index.less";
import Console, {CONSOLE_TOPIC} from "@/components/console";
import React, {KeyboardEvent, memo, useEffect, useReducer, useRef} from "react";
import {Button, Input} from "antd";
import {EnterOutlined, LoadingOutlined, ClearOutlined, CloseOutlined, RightOutlined, VerticalAlignBottomOutlined} from "@ant-design/icons";
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
import TextWrapIcon from "@/components/icons/TextWrapIcon";

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

interface SuperPanelState {
    view: string;
    executing: boolean;
    command: string;
    data: any;
    textWrap: boolean;
    autoScrollEnd: boolean;
}

const MAX_HISTORY = 100;
const historyMap = new Map<string, HistoryProp>();

const SuperPanel = memo((props: SuperPanelProps) => {
    const intl = useIntl();
    const initArg = {
        view: '',
        executing: false,
        command: '',
        data: {},
        textWrap: false,
        autoScrollEnd: true,
    } as SuperPanelState;
    const [state, dispatch] = useReducer((state: SuperPanelState, action: any) => {
        if ('function' === typeof action) {
            return {...state, ...action(state)};
        }
        return {...state, ...action};
    }, initArg, arg => ({...arg}));

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
        switch (state.view) {
            case 'dashboard':
                panel = <DashboardView data={state.data}/>;
                break;
            case 'jad':
                panel = <JadView data={state.data}/>;
                break;
            case 'heapdump':
                panel = <HeapDumpView data={state.data} remote={props.remote}/>;
                break;
            default:
                panel = <div>Unknown command view {state.view}</div>;
                break;
        }
        const buttonTitle = state.executing ? intl.formatMessage({id: 'CANCEL'}) : intl.formatMessage({id: 'CLOSE'});
        return (<div style={{height: JarBootConst.PANEL_HEIGHT}}>
            <TopTitleBar title={state.command}
                         icon={state.executing && <LoadingOutlined className={styles.statusStarting}/>}
                         onClose={closeView}
                         closeButtonTitle={buttonTitle}/>
            {panel}
        </div>);
    };

    const renderView = (resultData: any) => {
        const cmd = resultData.name;
        dispatch((preState: SuperPanelState) => {
            const curState = {} as SuperPanelState;
            if (cmd !== preState.view) {
                curState.view = cmd;
            }
            curState.data = resultData;
            return curState;
        });
    };

    const onExecQuickCmd = (cmd: string) => {
        if (inputRef?.current?.props?.disabled) {
            CommonNotice.info(intl.formatMessage({id: 'COMMAND_RUNNING'}, {command: inputRef.current.state?.value}));
            return;
        }
        if (StringUtil.isEmpty(cmd)) {
            return;
        }
        dispatch({command: cmd});
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
        dispatch({executing: false});
        pubsub.publish(key, CONSOLE_TOPIC.FINISH_LOADING, msg);
        onFocusCommandInput();
    };

    const clearDisplay = () => {
        pubsub.publish(key, CONSOLE_TOPIC.CLEAR_CONSOLE);
        inputRef?.current?.focus();
    };

    const closeView = () => {
        if (state.executing) {
            onCancelCommand();
            return;
        }
        if ('' !== state.view) {
            //切换为控制台显示
            dispatch({view: ''});
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
        dispatch((preState: SuperPanelState) => {
            const curState = {executing: true} as SuperPanelState;
            if ('' !== preState.view) {
                //切换为控制台显示
                curState.view = '';
            }
            return curState;
        });

        pubsub.publish(key, CONSOLE_TOPIC.APPEND_LINE, `<span class="${styles.commandPrefix}">$</span>${cmd}`);
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
        doExecCommand(state.command);
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
                dispatch({command: value});
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
                dispatch({command: value});
            }
        }
    };

    const consolePanel = () => {
        const style = '' === state.view ? {height: JarBootConst.PANEL_HEIGHT} : {display: 'none'};
        return (<div style={style}>
            <Console id={key}
                     pubsub={pubsub}
                     height={JarBootConst.PANEL_HEIGHT - 26}
                     wrap={state.textWrap}
                     autoScrollEnd={state.autoScrollEnd}/>
            <Input onPressEnter={onExecCommand}
                   onKeyUp={onKeyUp}
                   ref={inputRef}
                   className={styles.commandInput}
                   disabled={state.executing}
                   placeholder={intl.formatMessage({id: 'COMMAND_PLACEHOLDER'})}
                   autoComplete={"off"}
                   autoCorrect="off"
                   autoCapitalize="off"
                   spellCheck="false"
                   onChange={event => dispatch({command: event.target.value})}
                   value={state.command}
                   prefix={<RightOutlined className={styles.commandRightIcon}/>}
                   suffix={state.executing ? <LoadingOutlined/> : <EnterOutlined onClick={onExecCommand}/>}
            />
            {'' === state.view && extraButton()}
        </div>);
    };

    const setTextWrap = () => {
        dispatch((preState: SuperPanelState) => {
            const textWrap = !preState.textWrap;
            return {textWrap};
        });
    };

    const setScrollToEnd = () => {
        dispatch((preState: SuperPanelState) => {
            const autoScrollEnd = !preState.autoScrollEnd;
            if (autoScrollEnd) {
                //跳转到最后
                pubsub.publish(key, CONSOLE_TOPIC.SCROLL_TO_END);
            }
            return {autoScrollEnd};
        });
    };

    const extraButton = () => {
        let extra;
        const style = {fontSize: 16};
        if (state.executing) {
            extra = <Button icon={<CloseOutlined style={style}/>}
                            size={"small"}
                            title={intl.formatMessage({id: 'CLOSE'})}
                            ghost danger
                            onClick={onCancelCommand}/>;
        } else {
            extra = <Button icon={<ClearOutlined style={style}/>}
                            size={"small"}
                            title={intl.formatMessage({id: 'CLEAR'})}
                            ghost danger
                            onClick={clearDisplay}/>;
        }
        const wrap = <Button icon={<TextWrapIcon style={style}/>}
                             size={"small"}
                             title={intl.formatMessage({id: 'TEXT_WRAP'})}
                             ghost={!state.textWrap}
                             type={"primary"}
                             onClick={setTextWrap}/>;
        const scrollToEnd = <Button icon={<VerticalAlignBottomOutlined style={style}/>}
                                    size={"small"} title={intl.formatMessage({id: 'AUTO_SCROLL_END'})}
                                    ghost={!state.autoScrollEnd}
                                    type={"primary"}
                                    onClick={setScrollToEnd}/>;

        return (<div className={styles.consoleExtra}>{extra}{wrap}{scrollToEnd}</div>);
    };
    return (
        <div style={{display: props.visible ? 'block' : 'none'}}>
            {consolePanel()}
            {'' !== state.view && viewResolver()}
            {JarBootConst.IS_SAFARI && '' === state.view && <div className={styles.consoleScrollbarMaskForMac}/>}
        </div>);
});

export {SuperPanel};
