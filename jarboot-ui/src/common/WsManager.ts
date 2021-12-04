import Logger from "@/common/Logger";
import StringUtil from "@/common/StringUtil";
import { MSG_EVENT } from "@/common/EventConst";
import { JarBootConst, MsgData } from "@/common/JarBootConst";
import CommonNotice from "@/common/CommonNotice";
import { message } from 'antd';
import CommonUtils from "@/common/CommonUtils";
import { MessageType } from "antd/lib/message";

enum NoticeLevel {
    /** 提示 */
    INFO,

    /** 警告 */
    WARN,

    /** 错误 */
    ERROR
}

let msg: any = null;

class WsManager {
    public static readonly RECONNECTED_EVENT = -1;
    private static readonly HANDLERS = new Map<number, (data: MsgData) => void>();
    private static readonly LOADING_MAP = new Map<string, MessageType>();
    private static websocket: any = null;
    private static fd: any = null;

    public static addMessageHandler(key: number, handler: (data: MsgData) => void) {
        if (handler && !WsManager.HANDLERS.has(key)) {
            WsManager.HANDLERS.set(key, handler);
        }
    }

    public static clearHandlers() {
        WsManager.HANDLERS.clear();
    }

    public static removeMessageHandler(key: number) {
        if (key) {
            WsManager.HANDLERS.delete(key);
        }
    }

    public static sendMessage(text: string) {
        if (WsManager.websocket && WebSocket.OPEN === WsManager.websocket.readyState) {
            WsManager.websocket.send(text);
            return;
        }
        WsManager.initWebsocket();
    }

    public static initWebsocket() {
        WsManager.addMessageHandler(MSG_EVENT.NOTICE, WsManager.notice);
        WsManager.addMessageHandler(MSG_EVENT.GLOBAL_LOADING, WsManager.globalLoading);
        if (WsManager.websocket) {
            if (WebSocket.OPEN === WsManager.websocket.readyState ||
                WebSocket.CONNECTING === WsManager.websocket.readyState) {
                return;
            }
        }
        let url = process.env.NODE_ENV === 'development' ?
            `ws://${window.location.hostname}:9899/public/jarboot/service/ws?token=` :
            `ws://${window.location.host}/public/jarboot/service/ws?token=`;
        url += CommonUtils.getToken();
        WsManager.websocket = new WebSocket(url);
        WsManager.websocket.onmessage = WsManager.onMessage;
        WsManager.websocket.onopen = WsManager.onOpen;
        WsManager.websocket.onclose = WsManager.onClose;
        WsManager.websocket.onerror = WsManager.onError;
    }

    private static notice = (data: MsgData) => {
        const body: string = data.body;
        const index = body.indexOf(',');
        const level = parseInt(body.substring(0, index));
        const msg = body.substring(index + 1);
        switch (level) {
            case NoticeLevel.INFO:
                CommonNotice.info(msg);
                break;
            case NoticeLevel.WARN:
                CommonNotice.warn(msg);
                break;
            case NoticeLevel.ERROR:
                CommonNotice.error(msg);
                break;
            default:
                CommonNotice.error(`通知级别错误${level}`, msg);
                break;
        }
    }

    private static globalLoading = (data: MsgData) => {
        const body: string = data.body;
        if (StringUtil.isEmpty(body)) {
            return;
        }
        const index = body.indexOf(JarBootConst.PROTOCOL_SPLIT);
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

    private static onMessage = (e: any) => {
        if (!StringUtil.isString(e?.data)) {
            //二进制数据
            return;
        }
        const resp: string = e.data;
        try {
            let i = resp.indexOf(JarBootConst.PROTOCOL_SPLIT);
            if (-1 === i) {
                Logger.warn(`协议错误，${resp}`);
                return;
            }
            const sid = 0 === i ? '' : resp.substring(0, i);
            let k = resp.indexOf(JarBootConst.PROTOCOL_SPLIT, i + 1);
            if (-1 === k) {
                Logger.warn(`协议错误，获取事件类型失败，${resp}`);
                return;
            }
            const event = parseInt(resp.substring(i + 1, k));
            const body = resp.substring(k + 1);
            let data: MsgData = {event, body, sid};
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

export { WsManager }
