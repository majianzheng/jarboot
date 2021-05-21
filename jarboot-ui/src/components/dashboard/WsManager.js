
export default class WsManager {
    static websocket = null;
    static _messageHandler = new Set();
    static addMessageHandler(handler) {
        if (handler && !WsManager._messageHandler.has(handler)) {
            WsManager._messageHandler.add(handler);
        }
    }
    static removeMessageHandler(handler) {
        if (handler) {
            WsManager._messageHandler.delete(handler);
        }
    }
    static sendMessage(text) {
        if (WsManager.websocket && WebSocket.OPEN === WsManager.websocket.readyState) {
            WsManager.websocket.send(text);
            return;
        }
        WsManager.initWebsocket();
    }
    static initWebsocket() {
        if (WsManager.websocket) {
            if (WebSocket.OPEN === WsManager.websocket.readyState ||
                WebSocket.CONNECTING === WsManager.websocket.readyState) {
                return;
            }
        }
        let url = `ws://${window.location.hostname}:9899/jarboot-service/ws`;
        WsManager.websocket = new WebSocket(url);
        WsManager.websocket.onmessage = WsManager._onMessage;
        WsManager.websocket.onopen = () => console.log("连接Websocket服务器成功！");
        WsManager.websocket.onclose = () => console.log("websocket连接关闭！");
        WsManager.websocket.onerror = e => console.log(e);
    }
    static _onMessage = event => {
        try {
            WsManager._messageHandler.forEach(handler => WsManager._callHandler(handler, event.data));
        } catch (e) {
            console.log(e);
        }
    };
    static _callHandler(handler, data) {
        try {
            handler(data)
        } catch (e) {
            console.log(e);
        }
    }
}
