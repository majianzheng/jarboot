package com.mz.jarboot.ws;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.event.AttachStatus;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.event.WsEventEnum;
import com.mz.jarboot.task.TaskStatus;
import org.apache.commons.lang3.StringUtils;

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
    /** 是否初始化 */
    private boolean initialized = false;
    /** 会话存储 <会话ID，消息处理器> */
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

    /**
     * 获取单例
     * @return 单例
     */
    public static WebSocketManager getInstance() {
        return WebSocketManagerHolder.INSTANCE;
    }

    /**
     * 浏览器新连接
     * @param session 会话
     */
    public void newConnect(Session session) {
        sessionMap.put(session.getId(), new MessageQueueOperator(session));
    }

    /**
     * 浏览器连接断开
     * @param id 会话ID
     */
    public void delConnect(String id) {
        sessionMap.remove(id);
    }

    /**
     * 所有浏览器客户端的控制台打印一行
     * @param sid sid
     * @param text 文本
     */
    public void sendConsole(String sid, String text) {
        this.publishGlobalEvent(sid, text, WsEventEnum.CONSOLE);
    }

    /**
     * 浏览器控制台打印一行
     * @param sid sid
     * @param text 文本
     * @param sessionId 指定的浏览器会话
     */
    public void sendConsole(String sid, String text, String sessionId) {
        this.publishEvent(sid, text, sessionId, WsEventEnum.CONSOLE);
    }

    /**
     * 浏览器控制台打印文本
     * @param sid sid
     * @param text 文本
     * @param sessionId 指定的浏览器会话
     */
    public void stdPrint(String sid, String text, String sessionId) {
        this.publishEvent(sid, text, sessionId, WsEventEnum.STD_PRINT);
    }

    /**
     * 浏览器控制台退格num个字符
     * @param sid sid
     * @param num 退格字符数
     * @param sessionId 指定的浏览器会话
     */
    public void backspace(String sid, String num, String sessionId) {
        this.publishEvent(sid, num, sessionId, WsEventEnum.BACKSPACE);
    }

    /**
     * 特定命令执行结果的渲染
     * @param sid sid
     * @param text json字符串
     * @param sessionId 会话ID
     */
    public void renderJson(String sid, String text, String sessionId) {
        this.publishEvent(sid, text, sessionId, WsEventEnum.RENDER_JSON);
    }

    /**
     * 服务任务的状态变化事件
     * @param sid sid
     * @param status 状态
     */
    public void publishStatus(String sid, TaskStatus status) {
        //发布状态变化
        String msg = formatMsg(sid, WsEventEnum.SERVER_STATUS, status.name());
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    /**
     * 命令执行结束
     * @param sid sid
     * @param body 执行结果
     * @param sessionId 会话ID
     */
    public void commandEnd(String sid, String body, String sessionId) {
        this.publishEvent(sid, body, sessionId, WsEventEnum.CMD_END);
    }

    /**
     * 进程调试事件
     * @param sid sid
     * @param event 事件
     */
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

    /**
     * 浏览器弹出Notice通知
     * @param text 消息内容
     * @param level 通知级别
     */
    public void notice(String text, NoticeEnum level) {
        String msg;
        if (null != (msg = createNoticeMsg(text, level))) {
            sessionMap.forEach((k, v) -> v.newMessage(msg));
        }
    }

    /**
     * 浏览器弹出Notice通知
     * @param text 消息内容
     * @param level 通知级别
     * @param sessionId 指定会话
     */
    public void notice(String text, NoticeEnum level, String sessionId) {
        if (CommandConst.SESSION_COMMON.equals(sessionId)) {
            notice(text, level);
            return;
        }
        MessageQueueOperator operator;
        String msg;
        if (null != (msg = createNoticeMsg(text, level)) &&
                null != (operator = sessionMap.getOrDefault(sessionId, null))) {
            operator.newMessage(msg);
        }
    }

    /**
     * 向所有浏览器客户端发送事件
     * @param sid sid
     * @param body 消息体
     * @param event 事件
     */
    public void publishGlobalEvent(String sid, String body, WsEventEnum event) {
        String msg = formatMsg(sid, event, body);
        this.sessionMap.forEach((k, operator) -> operator.newMessage(msg));
    }

    /**
     * 打印异常到控制台
     * @param sid SID
     * @param e 异常
     */
    public void printException(String sid, Throwable e) {
        final byte lineBreak = '\n';
        sendConsole(sid, "\033[31m" + e.getMessage() + "\033[0m");
        e.printStackTrace(new PrintStream(new OutputStream() {
            private final byte[] buffer = new byte[1536];
            private int index = 0;
            @Override
            public void write(int b) {
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
        MessageQueueOperator.consumeMessage();
    }

    /**
     * 发布事件
     * @param sid sid
     * @param body 消息体
     * @param sessionId 会话ID
     * @param event 事件
     */
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

    /**
     * 创建Notice消息体
     * @param text 消息内容
     * @param level 消息级别
     * @return 消息体
     */
    private String createNoticeMsg(String text, NoticeEnum level) {
        if (StringUtils.isEmpty(text) || null == level) {
            return null;
        }
        //协议格式：level(0, 1, 2) + 逗号, + 消息内容
        String body = level.ordinal() + CommonConst.COMMA_SPLIT + text;
        return formatMsg(StringUtils.EMPTY, WsEventEnum.NOTICE, body);
    }

    /**
     * 前端交互协议封装
     * @param sid server id
     * @param event 事件
     * @param body 消息体
     * @return 封装后内容
     */
    private static String formatMsg(String sid, WsEventEnum event, String body) {
        StringBuilder sb = new StringBuilder();
        //使用\r作为分隔符
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
