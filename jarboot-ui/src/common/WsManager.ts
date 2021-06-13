import Logger from "@/common/Logger";
import StringUtil from "@/common/StringUtil";
import {MSG_EVENT} from "@/common/EventConst";
import {JarBootConst} from "@/common/JarBootConst";
import CommonNotice from "@/common/CommonNotice";
import { message } from 'antd';

interface MsgData {
    event: number,
    server: string,
    body: any
}
let msg: any = null;
class WsManager {
    private static websocket: any = null;
    private static fd: any = null;
    private static _messageHandler = new Map<number, (data: MsgData) => void>();

    public static addMessageHandler(key: number, handler: (data: MsgData) => void) {
        if (handler && !WsManager._messageHandler.has(key)) {
            WsManager._messageHandler.set(key, handler);
        }
    }

    public static clearHandlers() {
        WsManager._messageHandler.clear();
    }

    public static removeMessageHandler(key: number) {
        if (key) {
            WsManager._messageHandler.delete(key);
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
        WsManager.addMessageHandler(MSG_EVENT.NOTICE, WsManager._notice);
        if (WsManager.websocket) {
            if (WebSocket.OPEN === WsManager.websocket.readyState ||
                WebSocket.CONNECTING === WsManager.websocket.readyState) {
                return;
            }
        }
        let url = `ws://${window.location.hostname}:9899/jarboot-service/ws`;
        WsManager.websocket = new WebSocket(url);
        WsManager.websocket.onmessage = WsManager._onMessage;
        WsManager.websocket.onopen = WsManager._onOpen;
        WsManager.websocket.onclose = WsManager._onClose;
        WsManager.websocket.onerror = WsManager._onError;
    }

    private static _notice = (data: MsgData) => {
        const level = data.body?.level;
        const msg = data.body?.msg;
        switch (level) {
            case JarBootConst.NOTICE_INFO:
                CommonNotice.info("提示", msg);
                break;
            case JarBootConst.NOTICE_WARN:
                CommonNotice.warn("警告", msg);
                break;
            case JarBootConst.NOTICE_ERROR:
                CommonNotice.error("错误", msg);
                break;
            default:
                Logger.warn(`未知的通知类型：${level}, ${msg}`);
                break;
        }
    };

    private static _onMessage = (event: any) => {
        if (!StringUtil.isString(event.data)) {
            //二进制数据
            return;
        }
        try {
            const data: MsgData = JSON.parse(event.data);
            const handler = WsManager._messageHandler.get(data.event);
            handler && handler(data);
        } catch (e) {
            Logger.warn(e);
        }
    };

    private static _onOpen = () => {
        Logger.log("连接Websocket服务器成功！");
        msg && msg();
        msg = null;
        if (null !== WsManager.fd) {
            //连接成功，取消重连机制
            clearInterval(WsManager.fd);
            WsManager.fd = null;
        }
    };

    private static _onClose = () => {
        Logger.log("websocket连接关闭！");
        WsManager._reconnect();
    };

    private static _onError = (e: Error) => {
        Logger.log("websocket异常关闭！");
        Logger.error(e);
        WsManager._reconnect();
    };

    private static _reconnect() {
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
                clearInterval(WsManager.fd);
                WsManager.fd = null;
                return;
            }
            WsManager.initWebsocket();
        }, 10000);
    }
}

export {MsgData, WsManager}
