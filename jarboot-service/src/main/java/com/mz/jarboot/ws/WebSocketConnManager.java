package com.mz.jarboot.ws;

import com.alibaba.fastjson.JSON;
import com.mz.jarboot.constant.SettingConst;
import com.mz.jarboot.dto.MessageBodyDTO;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketConnManager {
    private static volatile WebSocketConnManager instance = null;
    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private WebSocketConnManager(){};
    public static WebSocketConnManager getInstance() {
        if (null == instance) {
            synchronized (WebSocketConnManager.class) {
                if (null == instance) {
                    instance = new WebSocketConnManager();
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
        this.notice(text, SettingConst.NOTICE_INFO);
    }
    public void noticeWarn(String text) {
        this.notice(text, SettingConst.NOTICE_WARN);
    }
    public void noticeError(String text) {
        this.notice(text, SettingConst.NOTICE_ERROR);
    }

    private void notice(String text, String type) {
        String msg = formatMsg("", type, text);
        if (!sessionMap.isEmpty()) {
            //当有人用网页访问时，只在网页界面提示
            this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
        }
    }

    private String formatMsg(String server, String msgType, String text) {
        MessageBodyDTO msgBody = new MessageBodyDTO();
        msgBody.setServer(server);
        msgBody.setServerType("WEB");
        msgBody.setMsgType(msgType);
        msgBody.setText(text);
        return JSON.toJSONString(msgBody);
    }
    private void sendTextMessage(final Session session, String msg) {
        MsgSendUtils.sendText(session, msg);
    }
}
