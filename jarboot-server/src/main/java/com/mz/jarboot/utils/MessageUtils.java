package com.mz.jarboot.utils;

import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.constant.NoticeLevel;
import com.mz.jarboot.event.BroadcastMessageEvent;
import com.mz.jarboot.event.FrontEndNotifyEventType;
import com.mz.jarboot.event.MessageEvent;
import com.mz.jarboot.task.AttachStatus;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 前端推送消息工具类
 * @author majianzheng
 */
public class MessageUtils {
    /**
     * 所有浏览器客户端的控制台打印一行
     * @param sid sid
     * @param text 文本
     */
    public static void console(String sid, String text) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(sid)
                        .body(text)
                        .type(FrontEndNotifyEventType.CONSOLE));
    }

    /**
     * 浏览器控制台打印一行
     * @param sid sid
     * @param sessionId 指定的浏览器会话
     * @param text 文本
     */
    public static void console(String sid, String sessionId, String text) {
        NotifyReactor
                .getInstance()
                .publishEvent(new MessageEvent(sid, sessionId)
                        .body(text)
                        .type(FrontEndNotifyEventType.CONSOLE));
    }

    /**
     * 浏览器控制台打印文本
     * @param sid sid
     * @param text 文本
     */
    public static void stdPrint(String sid, String text) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(sid)
                        .body(text)
                        .type(FrontEndNotifyEventType.STD_PRINT));
    }

    /**
     * 浏览器控制台退格num个字符
     * @param sid sid
     * @param num 退格字符数
     */
    public static void backspace(String sid, String num) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(sid)
                        .body(num)
                        .type(FrontEndNotifyEventType.BACKSPACE));
    }

    /**
     * 特定命令执行结果的渲染
     * @param sid sid
     * @param json json字符串
     * @param sessionId 会话ID
     */
    public static void render(String sid, String sessionId, String json) {
        NotifyReactor
                .getInstance()
                .publishEvent(new MessageEvent(sid, sessionId)
                        .body(json)
                        .type(FrontEndNotifyEventType.RENDER_JSON));
    }

    /**
     * 命令执行结束
     * @param sid sid
     * @param body 执行结果
     * @param sessionId 会话ID
     */
    public static void commandEnd(String sid, String sessionId, String body) {
        NotifyReactor
                .getInstance()
                .publishEvent(new MessageEvent(sid, sessionId)
                        .body(body)
                        .type(FrontEndNotifyEventType.CMD_END));
    }

    /**
     * 服务任务的状态变化事件
     * @param sid sid
     * @param status 状态
     */
    public static void upgradeStatus(String sid, String status) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(sid)
                        .body(status)
                        .type(FrontEndNotifyEventType.SERVER_STATUS));
    }

    /**
     * 服务任务的状态变化事件
     * @param sid sid
     * @param status 状态
     */
    public static void upgradeStatus(String sid, AttachStatus status) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(sid)
                        .body(status.name())
                        .type(FrontEndNotifyEventType.JVM_PROCESS_CHANGE));
    }

    /**
     * 向所有浏览器客户端发送事件
     * @param type 事件类型
     */
    public static void globalEvent(FrontEndNotifyEventType type) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(StringUtils.SPACE)
                        .type(type));
    }

    /**
     * 向所有浏览器客户端发送事件
     * @param body 消息体
     * @param type 事件类型
     */
    public static void globalEvent(String body, FrontEndNotifyEventType type) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(StringUtils.SPACE)
                        .body(body)
                        .type(type));
    }

    /**
     * 在浏览器界面弹出全局的loading提示，message为空时关闭
     * @param id 唯一id
     * @param message 消息提示，若为空{@link null}则关闭loading提示
     */
    public static void globalLoading(String id, String message) {
        if (StringUtils.isEmpty(id)) {
            return;
        }
        String body = id;
        if (StringUtils.isNotEmpty(message)) {
            body += StringUtils.CR + message;
        }
        globalEvent(body, FrontEndNotifyEventType.GLOBAL_LOADING);
    }

    /**
     * info
     * @param msg msg
     */
    public static void info(String msg) {
        notice(msg, NoticeLevel.INFO);
    }

    /**
     * warn
     * @param msg msg
     */
    public static void warn(String msg) {
        notice(msg, NoticeLevel.WARN);
    }

    /**
     * error
     * @param msg msg
     */
    public static void error(String msg) {
        notice(msg, NoticeLevel.ERROR);
    }

    public static void notice(String msg, NoticeLevel level) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(StringUtils.SPACE)
                        .body(msg, level));
    }

    /**
     * info
     * @param sessionId session id
     * @param msg msg
     */
    public static void info(String sessionId, String msg) {
        notice(sessionId, msg, NoticeLevel.INFO);
    }

    /**
     * warn
     * @param sessionId session id
     * @param msg msg
     */
    public static void warn(String sessionId, String msg) {
        notice(sessionId, msg, NoticeLevel.WARN);
    }

    /**
     * error
     * @param sessionId session id
     * @param msg msg
     */
    public static void error(String sessionId, String msg) {
        notice(sessionId, msg, NoticeLevel.ERROR);
    }

    /**
     * error
     * @param sessionId session id
     * @param msg msg
     */
    public static void notice(String sessionId, String msg, NoticeLevel level) {
        NotifyReactor
                .getInstance()
                .publishEvent(new MessageEvent(StringUtils.SPACE, sessionId)
                        .body(msg, level));
    }

    /**
     * 打印异常到控制台
     * @param sid SID
     * @param e 异常
     */
    public static void printException(String sid, Throwable e) {
        final byte lineBreak = '\n';
        console(sid, "\033[31m" + e.getMessage() + "\033[0m");
        e.printStackTrace(new PrintStream(new OutputStream() {
            private final byte[] buffer = new byte[1536];
            private int index = 0;
            @Override
            public void write(int b) {
                byte c = (byte) b;
                if (lineBreak == c) {
                    console(sid, new String(this.buffer, 0, this.index, StandardCharsets.UTF_8));
                    this.index = 0;
                } else {
                    buffer[this.index++] = c;
                }
            }
        }));
    }

    private MessageUtils() {}
}
