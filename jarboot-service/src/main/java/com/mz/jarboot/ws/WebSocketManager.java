package com.mz.jarboot.ws;

import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.constant.CommonConst;
import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketManager {
    private static volatile WebSocketManager instance = null;
    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private WebSocketManager(){};
    public static WebSocketManager getInstance() {
        if (null == instance) {
            synchronized (WebSocketManager.class) {
                if (null == instance) {
                    instance = new WebSocketManager();
                }
            }
        }
        return instance;
    }
    public void addNewConnect(Session session) {
        sessionMap.put(session.getId(), session);
    }
    public void delConnect(String id) {
        sessionMap.remove(id);
    }

    public void sendOutMessage(String server, String text) {
        String msg = formatMsg(server, "OUT", text);
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }
    public void sendStopMessage(String server, boolean finished) {
        String msg = formatMsg(server, finished ? "STOPPED" : "STOP", "");
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }
    public void sendStartMessage(String server) {
        String msg = formatMsg(server, "START", "");
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }
    public void sendStartedMessage(String server, int pid) {
        String msg = formatMsg(server,  "STARTED", String.valueOf(pid));
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }
    public void sendStartErrorMessage(String server) {
        String msg = formatMsg(server, "START_ERROR", "");
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }
    public void sendStopErrorMessage(String server) {
        String msg = formatMsg(server, "STOP_ERROR", "");
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }
    public void noticeInfo(String text) {
        this.notice(text, CommonConst.NOTICE_INFO);
    }
    public void noticeWarn(String text) {
        this.notice(text, CommonConst.NOTICE_WARN);
    }
    public void noticeError(String text) {
        this.notice(text, CommonConst.NOTICE_ERROR);
    }

    private void notice(String text, String type) {
        String msg = formatMsg(null, type, text);
        if (!sessionMap.isEmpty()) {
            this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
        }
    }

    private String formatMsg(String server, String msgType, String text) {
        JSONObject json = new JSONObject();
        json.put("server", server);
        json.put("msgType", msgType);
        json.put("text", text);
        return json.toJSONString();
    }
    private void sendTextMessage(final Session session, String msg) {
        MsgSendUtils.sendText(session, msg);
    }
}
