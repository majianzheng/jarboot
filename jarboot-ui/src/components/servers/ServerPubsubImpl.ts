import { WsManager } from "@/common/WsManager";
import { MSG_EVENT } from "@/common/EventConst";
import { CommonConst, MsgData } from "@/common/CommonConst";
import Logger from "@/common/Logger";
import {CONSOLE_TOPIC} from "@/components/console";

/**
 * 服务订阅发布实现
 */
const TOPIC_SPLIT = '\r';

enum PUB_TOPIC {
    ROOT = "root",
    CMD_END = "commandEnd",
    RENDER_JSON = "renderJson",
    QUICK_EXEC_CMD = "quickExecCmd",
    RECONNECTED = "reconnected",
    WORKSPACE_CHANGE = "workspaceChange",
    STATUS_CHANGE = "statusChange",
    FOCUS_CMD_INPUT = "focusCmdInput",
    ONLINE_DEBUG_EVENT = "onlineDebugEvent",
}

class ServerPubsubImpl implements PublishSubmit {
    private handlers = new Map<string, Set<(data: any) => void>>();

    constructor() {
        WsManager.addMessageHandler(MSG_EVENT.CONSOLE_LINE, this.console);
        WsManager.addMessageHandler(MSG_EVENT.CONSOLE_PRINT, this.stdPrint);
        WsManager.addMessageHandler(MSG_EVENT.BACKSPACE, this.backspace);
        WsManager.addMessageHandler(MSG_EVENT.RENDER_JSON, this.renderCmdJsonResult);
        WsManager.addMessageHandler(MSG_EVENT.CMD_END, this.commandEnd);
        WsManager.addMessageHandler(MSG_EVENT.WORKSPACE_CHANGE, this.workspaceChange);
        WsManager.addMessageHandler(WsManager.RECONNECTED_EVENT, this.onReconnected);
        WsManager.addMessageHandler(MSG_EVENT.SERVER_STATUS, this.statusChange);
        WsManager.addMessageHandler(MSG_EVENT.JVM_PROCESS_CHANGE, this.onJvmProcessChange);
    }

    private static genTopicKey(namespace: string, event: string|number) {
        return `${namespace}${TOPIC_SPLIT}${event}`;
    }

    public publish(namespace: string, event: string|number, data?: any): void {
        const key = ServerPubsubImpl.genTopicKey(namespace, event);
        let sets = this.handlers.get(key);
        if (sets?.size) {
            sets.forEach(handler => handler && handler(data));
        }
    }

    public submit(namespace: string, event: string|number, handler: (data: any) => void): void {
        const key = ServerPubsubImpl.genTopicKey(namespace, event);
        let sets = this.handlers.get(key);
        if (sets?.size) {
            sets.add(handler);
        } else {
            sets = new Set<(data: any) => void>();
            sets.add(handler);
            this.handlers.set(key, sets);
        }
    }

    public unSubmit(namespace: string, event: string|number, handler: (data: any) => void): void {
        const key = ServerPubsubImpl.genTopicKey(namespace, event);
        const sets = this.handlers.get(key);
        if (sets?.size) {
            sets.delete(handler);
            if (sets.size === 0) {
                this.handlers.delete(key);
            }
        }
    }

    private console = (data: MsgData) => {
        this.publish(data.sid, CONSOLE_TOPIC.APPEND_LINE, data.body);
    };

    private stdPrint = (data: MsgData) => {
        this.publish(data.sid, CONSOLE_TOPIC.STD_PRINT, data.body);
    };

    private backspace = (data: MsgData) => {
        this.publish(data.sid, CONSOLE_TOPIC.BACKSPACE, data.body);
    };

    private commandEnd = (data: MsgData) => {
        this.publish(data.sid, PUB_TOPIC.CMD_END, data.body);
    };

    private workspaceChange = (data: MsgData) => {
        this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, data.body);
        Logger.log(`工作空间已经被修改，服务列表将会被刷新！`);
    };

    private statusChange = (data: MsgData) => {
        this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, data);
    };

    private onReconnected = (data: MsgData) => {
        this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, data.body);
        Logger.log(`重新连接服务成功，服务列表将会被刷新！`);
    };

    private onJvmProcessChange = (data: MsgData) => {
        this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, data);
    };

    private renderCmdJsonResult = (data: MsgData) => {
        if ('{' !== data.body[0]) {
            //不是json数据时，使用console
            Logger.warn(`当前非JSON数据格式！`, data);
            this.console(data);
            return;
        }
        const body = JSON.parse(data.body);
        this.publish(data.sid, PUB_TOPIC.RENDER_JSON, body);
    };
}

const pubsub: PublishSubmit = new ServerPubsubImpl();

export {pubsub, PUB_TOPIC};
