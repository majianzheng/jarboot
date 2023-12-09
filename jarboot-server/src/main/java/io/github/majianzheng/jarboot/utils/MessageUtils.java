package io.github.majianzheng.jarboot.utils;

import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.protocol.NotifyType;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.event.BroadcastMessageEvent;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.event.MessageEvent;
import io.github.majianzheng.jarboot.task.AttachStatus;

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
        notify(sid, true, NotifyType.CONSOLE.body(text));
    }

    /**
     * 浏览器控制台打印一行
     * @param sid sid
     * @param sessionId 指定的浏览器会话
     * @param text 文本
     */
    public static void console(String sid, String sessionId, String text) {
        notify(sid, sessionId, true, NotifyType.CONSOLE.body(text));
    }

    /**
     * 浏览器控制台打印文本
     * @param sessionIds session ids
     * @param sid sid
     * @param text 文本
     */
    public static void stdPrint(String sessionIds, String sid, String text) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(sessionIds, sid)
                        .body(text)
                        .type(FrontEndNotifyEventType.STD_PRINT));
    }

    /**
     * 命令执行失败
     * @param sid sid
     * @param body 执行结果
     * @param sessionId 会话ID
     */
    public static void commandFailed(String sid, String sessionId, String body) {
        notify(sid, sessionId, false, NotifyType.COMMAND_END.body(body));
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
        notice(msg, NotifyType.INFO);
    }

    /**
     * warn
     * @param msg msg
     */
    public static void warn(String msg) {
        notice(msg, NotifyType.WARN);
    }

    /**
     * error
     * @param msg msg
     */
    public static void error(String msg) {
        notice(msg, NotifyType.ERROR);
    }

    private static void notice(String msg, NotifyType level) {
        notify(StringUtils.SPACE, true, level.body(msg));
    }

    /**
     * info
     * @param sessionId session id
     * @param msg msg
     */
    public static void info(String sessionId, String msg) {
        notice(sessionId, msg, NotifyType.INFO);
    }

    /**
     * warn
     * @param sessionId session id
     * @param msg msg
     */
    public static void warn(String sessionId, String msg) {
        notice(sessionId, msg, NotifyType.WARN);
    }

    /**
     * error
     * @param sessionId session id
     * @param msg msg
     */
    public static void error(String sessionId, String msg) {
        notice(sessionId, msg, NotifyType.ERROR);
    }

    /**
     * error
     * @param sessionId session id
     * @param msg msg
     */
    private static void notice(String sessionId, String msg, NotifyType level) {
        notify(StringUtils.SPACE, sessionId, true, level.body(msg));
    }

    /**
     * notify
     * @param sid sid
     * @param sessionId session id
     * @param success 是否成功
     * @param body body
     */
    public static void notify(String sid, String sessionId, boolean success, String body) {
        NotifyReactor
                .getInstance()
                .publishEvent(new MessageEvent(sid, sessionId)
                        .type(FrontEndNotifyEventType.NOTIFY)
                        .body(wrapNotifyBody(success, body)));
    }

    /**
     * notify
     * @param sid sid
     * @param success 是否成功
     * @param body body
     */
    public static void notify(String sid, boolean success, String body) {
        NotifyReactor
                .getInstance()
                .publishEvent(new BroadcastMessageEvent(sid)
                        .type(FrontEndNotifyEventType.NOTIFY)
                        .body(wrapNotifyBody(success, body)));
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

    private static String wrapNotifyBody(boolean success, String body) {
        char flag = success ? '0' : '1';
        return flag + body;
    }

    private MessageUtils() {}
}
