package com.mz.jarboot.ws;

import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.event.WsEventEnum;
import com.mz.jarboot.task.TaskStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.concurrent.*;

/**
 * 与浏览器交互的WebSocket管理
 * @author majianzheng
 */
public class WebSocketManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

    @SuppressWarnings("all")
    private static volatile WebSocketManager instance = null;
    private volatile boolean initialized = false;
    private final ConcurrentHashMap<String, MessageQueueOperator> sessionMap = new ConcurrentHashMap<>(32);
    /**
     * 消费推送到前端的消息的线程组，执行流程如下
     * ┌─────────────┐  Push to   ┌────────────────┐ Note:
     * │ New Message │———————————▶│ Blocking Queue │ One session has on blocking queue
     * └─────────────┘            └────────────────┘
     *                                    │ Take
     *                                    ▼
     *                            ┌──────────────────┐
     *                            │ Consumer thread  │
     *                            └──────────────────┘
     */

    private WebSocketManager() {
        //初始化线程
        setDaemon(true);
        setName("jarboot.msg-consumer" );
        start();
    }

    @Override
    public synchronized void start() {
        if (!initialized) {
            // start just called once
            super.start();
            initialized = true;
        }
    }

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

    public void newConnect(Session session) {
        sessionMap.put(session.getId(), new MessageQueueOperator(session));
    }

    public void delConnect(String id) {
        sessionMap.remove(id);
    }

    public void sendConsole(String sid, String text) {
        this.publishGlobalEvent(sid, text, WsEventEnum.CONSOLE_LINE);
    }

    public void sendConsole(String sid, String text, String sessionId) {
        this.publishEvent(sid, text, sessionId, WsEventEnum.CONSOLE_LINE);
    }
    
    public void sendPrint(String sid, String text, String sessionId) {
        this.publishEvent(sid, text, sessionId, WsEventEnum.CONSOLE_PRINT);
    }
    
    public void backspace(String sid, String num, String sessionId) {
        this.publishEvent(sid, num, sessionId, WsEventEnum.BACKSPACE);
    }

    public void backspaceLine(String sid, String text, String sessionId) {
        this.publishEvent(sid, text, sessionId, WsEventEnum.BACKSPACE_LINE);
    }

    public void renderJson(String sid, String text, String sessionId) {
        this.publishEvent(sid, text, sessionId, WsEventEnum.RENDER_JSON);
    }

    public void publishStatus(String sid, TaskStatus status) {
        //发布状态变化
        String msg = formatMsg(sid, WsEventEnum.SERVER_STATUS, status.name());
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    public void commandEnd(String sid, String body, String sessionId) {
        this.publishEvent(sid, body, sessionId, WsEventEnum.CMD_END);
    }

    public void notice(String text, NoticeEnum level) {
        if (StringUtils.isEmpty(text) || null == level) {
            return;
        }
        WsEventEnum type = null;
        switch (level) {
            case INFO:
                type = WsEventEnum.NOTICE_INFO;
                break;
            case WARN:
                type = WsEventEnum.NOTICE_WARN;
                break;
            case ERROR:
                type = WsEventEnum.NOTICE_ERROR;
                break;
            default:
                return;
        }
        String msg = formatMsg(StringUtils.EMPTY, type, text);
        if (!sessionMap.isEmpty()) {
            this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
        }
    }

    @Override
    public void run() {
        BlockingQueue<MessageSender> queue = MessageQueueOperator.getQueue();
        for (; ; ) {
            try {
                final MessageSender sender = queue.take();
                sender.sendText();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void publishEvent(String sid, String body, String sessionId, WsEventEnum event) {
        if (CommandConst.SESSION_COMMON.equals(sessionId)) {
            //广播session的id
            publishGlobalEvent(sid, body, event);
            return;
        }
        MessageQueueOperator operator = this.sessionMap.getOrDefault(sessionId, null);
        if (null != operator) {
            String msg = formatMsg(sid, event, body);
            operator.newMessage(msg);
        }
    }

    public void publishGlobalEvent(String sid, String body, WsEventEnum event) {
        String msg = formatMsg(sid, event, body);
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    private static String formatMsg(String sid, WsEventEnum event, String body) {
        StringBuilder sb = new StringBuilder();
        sb
                .append(sid)
                .append(CommandConst.PROTOCOL_SPLIT)
                .append(event.ordinal())
                .append(CommandConst.PROTOCOL_SPLIT)
                .append(body);
        return sb.toString();
    }
}
