import Logger from "@/common/Logger";
import StringUtil from "@/common/StringUtil";
import { MSG_EVENT } from "@/common/EventConst";
import {FuncCode, CommonConst, MsgData, MsgReq} from "@/common/CommonConst";
import { message } from 'antd';
import CommonUtils from "@/common/CommonUtils";
import { MessageType } from "antd/lib/message";

enum NotifyType {
    /** 提示 */
    INFO,
    /** 警告 */
    WARN,
    /** 错误 */
    ERROR,
    /** 控制台消息打印 */
    CONSOLE,
    /** 执行执行完成 */
    COMMAND_END,
    /** Json类型的执行结果 */
    JSON_RESULT,
}

let msg: any = null;

/**
 * Websocket实现类
 * @author majianzheng
 */
class WsManager {
    /** 重连成功事件 */
    public static readonly RECONNECTED_EVENT = -1;
    /** 事件处理回调 */
    private static readonly HANDLERS = new Map<number, (data: MsgData) => void>();
    /** 全局Loading事件 */
    private static readonly LOADING_MAP = new Map<string, MessageType>();
    /** websocket句柄 */
    private static websocket: any = null;
    /** 重连setInterval的句柄 */
    private static fd: any = null;

    /**
     * 添加消息处理
     * @param key 事件
     * @param handler 事件处理回调
     */
    public static addMessageHandler(key: number, handler: (data: MsgData) => void) {
        if (handler && !WsManager.HANDLERS.has(key)) {
            WsManager.HANDLERS.set(key, handler);
        }
    }

    /**
     * 清理所有消息处理句柄
     */
    public static clearHandlers() {
        WsManager.HANDLERS.clear();
    }

    /**
     * 移除消息处理
     * @param key 事件
     */
    public static removeMessageHandler(key: number) {
        if (key) {
            WsManager.HANDLERS.delete(key);
        }
    }

    /**
     * 调用后端功能
     * @param func 功能id
     * @param sid sid
     */
    public static callFunc(func: FuncCode, sid: string) {
        const msg = {service: '', sid, body: '', func};
        WsManager.sendMessage(msg);
    }

    /**
     * 发送消息
     * @param msgReq 消息内容
     */
    public static sendMessage(msgReq: MsgReq) {
        if (WsManager.websocket && WebSocket.OPEN === WsManager.websocket.readyState) {
            const text = JSON.stringify(msgReq);
            if (text.length > 2048) {
                Logger.error("send message exceed max length.", text);
                return;
            }
            WsManager.websocket.send(text);
            return;
        }
        Logger.error('Websocket is not open, send failed.', msgReq);
        WsManager.initWebsocket();
    }

    /**
     * 初始化Websocket
     */
    public static initWebsocket() {
        WsManager.addMessageHandler(MSG_EVENT.GLOBAL_LOADING, WsManager.globalLoading);
        if (WsManager.websocket) {
            if (WebSocket.OPEN === WsManager.websocket.readyState ||
                WebSocket.CONNECTING === WsManager.websocket.readyState) {
                return;
            }
        }
        const token = `${CommonUtils.ACCESS_TOKEN}=${CommonUtils.getRawToken()}`;
        const url = process.env.NODE_ENV === 'development' ?
            `ws://${window.location.hostname}:9899/jarboot/main/service/ws?${token}` :
            `ws://${window.location.host}/jarboot/main/service/ws?${token}`;
        WsManager.websocket = new WebSocket(url);
        WsManager.websocket.onmessage = WsManager.onMessage;
        WsManager.websocket.onopen = WsManager.onOpen;
        WsManager.websocket.onclose = WsManager.onClose;
        WsManager.websocket.onerror = WsManager.onError;
    }

    /**
     * 全局Loading消息处理
     * @param data 消息
     */
    private static globalLoading = (data: MsgData) => {
        const body: string = data.body;
        if (StringUtil.isEmpty(body)) {
            return;
        }
        const index = body.indexOf(CommonConst.PROTOCOL_SPLIT);
        const hasSplit = -1 === index;
        const key = hasSplit ? body : body.substring(0, index);
        const handle = WsManager.LOADING_MAP.get(key);
        WsManager.LOADING_MAP.delete(key);
        if (hasSplit) {
            handle && handle();
        } else {
            const duration = 0;
            const content = body.substring(index + 1);
            WsManager.LOADING_MAP.set(key, message.loading({content, key, duration}, duration));
        }
    };

    /**
     * 响应后端消息推送处理
     * @param e 事件
     */
    private static onMessage = (e: MessageEvent) => {
        if (!StringUtil.isString(e?.data)) {
            Logger.error('Unknown websocket message:', e);
            //二进制数据
            return;
        }
        const resp: string = e.data;
        if ('ping' === resp) {
            return;
        }
        try {
            let i = resp.indexOf(CommonConst.PROTOCOL_SPLIT);
            if (-1 === i) {
                Logger.warn(`协议错误，${resp}`);
                return;
            }
            const sid = 0 === i ? '' : resp.substring(0, i);
            let k = resp.indexOf(CommonConst.PROTOCOL_SPLIT, i + 1);
            if (-1 === k) {
                Logger.warn(`协议错误，获取事件类型失败，${resp}`);
                return;
            }
            const event = parseInt(resp.substring(i + 1, k));
            const body = resp.substring(k + 1);
            const data: MsgData = {event, body, sid};
            const handler = WsManager.HANDLERS.get(data.event);
            handler && handler(data);
        } catch (error) {
            Logger.warn(error);
        }
    };

    private static onOpen = () => {
        Logger.log("连接Websocket服务器成功！");
        if (msg) {
            msg();
            msg = null;
            //重连成功，刷新状态
            const handler = WsManager.HANDLERS.get(WsManager.RECONNECTED_EVENT);
            handler && handler({sid: '', body: '', event: WsManager.RECONNECTED_EVENT});
        }
        if (null !== WsManager.fd) {
            //连接成功，取消重连机制
            clearInterval(WsManager.fd);
            WsManager.fd = null;
        }
        setTimeout(WsManager.ping, 300000);
    };

    private static onClose = () => {
        Logger.log("websocket连接关闭！");
        WsManager.reconnect();
    };

    private static onError = (e: Error) => {
        Logger.log("websocket异常关闭！");
        Logger.error(e);
        WsManager.reconnect();
    };

    private static ping = () => {
        if (WsManager.websocket && WebSocket.OPEN === WsManager.websocket.readyState) {
            WsManager.websocket.send('ping');
            setTimeout(WsManager.ping, 300000);
        }
    };

    private static reconnect() {
        if (null !== WsManager.fd) {
            return;
        }
        msg = message.loading('reconnecting...', 0);
        WsManager.fd = setInterval(() => {
            if (null === WsManager.fd) {
                //已经进入连onOpen
                return;
            }
            if (WebSocket.CONNECTING === WsManager.websocket?.readyState) {
                //正在连接，下一周期再次查看
                return;
            }
            if (WebSocket.OPEN === WsManager.websocket?.readyState) {
                //连接成功
                Logger.log("websocket重连成功！");
                clearInterval(WsManager.fd);
                WsManager.fd = null;
                return;
            }
            WsManager.initWebsocket();
        }, 15000);
    }
}

export { WsManager, NotifyType }
