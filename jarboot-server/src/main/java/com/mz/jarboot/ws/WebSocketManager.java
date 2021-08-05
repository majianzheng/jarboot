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

    public void sendConsole(String server, String text) {
        String msg = formatMsg(server, WsEventEnum.CONSOLE_LINE, text);
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    public void sendConsole(String server, String text, String sessionId) {
        if (CommandConst.SESSION_COMMON.equals(sessionId)) {
            //广播session的id
            sendConsole(server, text);
            return;
        }
        MessageQueueOperator operator = this.sessionMap.getOrDefault(sessionId, null);
        if (null != operator) {
            String msg = formatMsg(server, WsEventEnum.CONSOLE_LINE, text);
            operator.newMessage(msg);
        }
    }

    public void renderJson(String server, String text) {
        String msg = formatMsg(server, WsEventEnum.RENDER_JSON, text);
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    public void renderJson(String server, String text, String sessionId) {
        if (CommandConst.SESSION_COMMON.equals(sessionId)) {
            //广播session的id
            renderJson(server, text);
            return;
        }
        MessageQueueOperator operator = this.sessionMap.getOrDefault(sessionId, null);
        if (null != operator) {
            String msg = formatMsg(server, WsEventEnum.RENDER_JSON, text);
            operator.newMessage(msg);
        }
    }

    public void publishStatus(String server, TaskStatus status) {
        //发布状态变化
        String msg = formatMsg(server, WsEventEnum.SERVER_STATUS, status.name());
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    public void commandEnd(String server, String body) {
        String msg = formatMsg(server, WsEventEnum.CMD_END, body);
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    public void commandEnd(String server, String body, String sessionId) {
        if (CommandConst.SESSION_COMMON.equals(sessionId)) {
            //广播session的id
            commandEnd(server, body);
            return;
        }
        MessageQueueOperator operator = this.sessionMap.getOrDefault(sessionId, null);
        if (null != operator) {
            String msg = formatMsg(server, WsEventEnum.CMD_END, body);
            operator.newMessage(msg);
        }
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

    private static String formatMsg(String server, WsEventEnum event, String body) {
        StringBuilder sb = new StringBuilder();
        sb
                .append(server)
                .append(CommandConst.PROTOCOL_SPLIT)
                .append(event.ordinal())
                .append(CommandConst.PROTOCOL_SPLIT)
                .append(body);
        return sb.toString();
    }
}
