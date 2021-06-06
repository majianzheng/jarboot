package com.mz.jarboot.ws;

import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.event.WsMsgEventEnum;
import com.mz.jarboot.task.TaskStatus;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketManager {
    private static volatile WebSocketManager instance = null;// NOSONAR
    private final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private WebSocketManager(){}

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

    public void sendConsole(String server, String text) {
        String msg = formatMsg(server, WsMsgEventEnum.CONSOLE_LINE, text);
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }

    public void sendConsole(String server, String text, String sessionId) {
        if (CommandConst.SESSION_COMMON.equals(sessionId)) {
            //广播session的id
            sendConsole(server, text);
            return;
        }
        Session session = this.sessionMap.getOrDefault(sessionId, null);
        if (null != session) {
            String msg = formatMsg(server, WsMsgEventEnum.CONSOLE_LINE, text);
            sendTextMessage(session, msg);
        }
    }

    public void publishStatus(String server, TaskStatus status) {
        //发布状态变化
        String msg = formatMsg(server, WsMsgEventEnum.SERVER_STATUS, status.name());
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }

    public void commandComplete(String server) {
        String msg = formatMsg(server, WsMsgEventEnum.CMD_COMPLETE, null);
        this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
    }

    public void commandComplete(String server, String sessionId) {
        if (CommandConst.SESSION_COMMON.equals(sessionId)) {
            //广播session的id
            commandComplete(server);
            return;
        }
        Session session = this.sessionMap.getOrDefault(sessionId, null);
        if (null != session) {
            String msg = formatMsg(server, WsMsgEventEnum.CMD_COMPLETE, null);
            sendTextMessage(session, msg);
        }
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

    private void notice(String text, int type) {
        JSONObject json = new JSONObject();
        json.put("level", type);
        json.put("msg", text);
        String msg = formatMsg(null, WsMsgEventEnum.NOTICE, json);
        if (!sessionMap.isEmpty()) {
            this.sessionMap.forEach((k, session) -> sendTextMessage(session, msg));
        }
    }

    private static String formatMsg(String server, WsMsgEventEnum event, Object body) {
        JSONObject json = new JSONObject();
        json.put("server", server);
        json.put("event", event.ordinal());
        json.put("body", body);
        return json.toJSONString();
    }
    private static void sendTextMessage(final Session session, String msg) {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {// NOSONAR
            try {
                session.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                //ignore
            }
        }
    }
}
