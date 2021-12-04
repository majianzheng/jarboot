package com.mz.jarboot.ws;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.event.AttachStatus;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.event.WsEventEnum;
import com.mz.jarboot.task.TaskStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * 与浏览器交互的WebSocket管理
 * @author majianzheng
 */
public class WebSocketManager extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

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
        return WebSocketManagerHolder.INSTANCE;
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

    public void sendPrint(String sid, String text) {
        this.publishGlobalEvent(sid, text, WsEventEnum.CONSOLE_PRINT);
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

    public void debugProcessEvent(String sid, AttachStatus event) {
        this.publishGlobalEvent(sid, event.name(), WsEventEnum.JVM_PROCESS_CHANGE);
    }

    /**
     * 在浏览器界面弹出全局的loading提示，message为空时关闭
     * @param id 唯一id
     * @param message 消息提示，若为空{@link null}则关闭loading提示
     */
    public void globalLoading(String id, String message) {
        if (StringUtils.isEmpty(id)) {
            return;
        }
        String body = id;
        if (StringUtils.isNotEmpty(message)) {
            body += CommandConst.PROTOCOL_SPLIT + message;
        }
        this.publishGlobalEvent(StringUtils.EMPTY, body, WsEventEnum.GLOBAL_LOADING);
    }

    public void notice(String text, NoticeEnum level) {
        String msg;
        if (null != (msg = createNoticeMsg(text, level))) {
            sessionMap.forEach((k, v) -> v.newMessage(msg));
        }
    }

    public void notice(String text, NoticeEnum level, String sessionId) {
        MessageQueueOperator operator;
        String msg;
        if (null != (msg = createNoticeMsg(text, level)) &&
                null != (operator = sessionMap.getOrDefault(sessionId, null))) {
            operator.newMessage(msg);
        }
    }

    private String createNoticeMsg(String text, NoticeEnum level) {
        if (StringUtils.isEmpty(text) || null == level) {
            return null;
        }
        String body = level.ordinal() + CommonConst.COMMA_SPLIT + text;
        return formatMsg(StringUtils.EMPTY, WsEventEnum.NOTICE, body);
    }

    public void printException(String sid, Throwable e) {
        final byte lineBreak = '\n';
        e.printStackTrace(new PrintStream(new OutputStream() {
            private final byte[] buffer = new byte[1536];
            private int index = 0;
            @Override
            public void write(int b) {
                if (this.index > (this.buffer.length - 1)) {
                    sendPrint(sid, new String(this.buffer));
                    this.index = 0;
                }
                byte c = (byte) b;
                if (lineBreak == c) {
                    sendConsole(sid, new String(this.buffer, 0, this.index, StandardCharsets.UTF_8));
                    this.index = 0;
                } else {
                    buffer[this.index++] = c;
                }
            }
        }));
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
                break;
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

    private static class WebSocketManagerHolder {
        static final WebSocketManager INSTANCE = new WebSocketManager();
    }
}
