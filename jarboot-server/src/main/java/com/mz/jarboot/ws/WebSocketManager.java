package com.mz.jarboot.ws;

import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.constant.CommonConst;
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
 * @author jianzhengma
 */
public class WebSocketManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

    private static volatile WebSocketManager instance = null;// NOSONAR
    private final ConcurrentHashMap<String, MessageQueueOperator> sessionMap = new ConcurrentHashMap<>();
    /**
     * 消费推送到前端的消息的线程组，执行流程如下
     * ┌─────────────┐  Push to   ┌────────────────┐ Note:
     * │ New Message │———————————▶│ Blocking Queue │ One session has on blocking queue
     * └─────────────┘            └────────────────┘
     *                                    │ Take and wait some time when empty
     *                                    ▼
     *                            ┌──────────────────┐
     *                            │ Consumer threads │ When empty and timeout, then release to thread pool.
     *                            └──────────────────┘
     */
    private final ThreadPoolExecutor msgConsumerExecutor = new ThreadPoolExecutor(
            16, 32, 30L,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),
            (Runnable r, ThreadPoolExecutor executor) -> logger.warn("线程忙碌，无法响应发送消息请求！"));

    private WebSocketManager(){
        // 允许core线程数使用keepAliveTime，为节约CPU负荷，长时间空闲时释放线程
        msgConsumerExecutor.allowCoreThreadTimeOut(true);
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

    public void addNewConnect(Session session) {
        sessionMap.put(session.getId(), new MessageQueueOperator(session));
    }

    public void delConnect(String id) {
        sessionMap.remove(id);
    }

    public void sendConsole(String server, String text) {
        String msg = formatMsg(server, WsEventEnum.CONSOLE_LINE, text);
        this.sessionMap.forEach((k, operator) -> messageProducer(operator, msg));
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
            messageProducer(operator, msg);
        }
    }

    public void renderJson(String server, String text) {
        String msg = formatMsg(server, WsEventEnum.RENDER_JSON, text);
        this.sessionMap.forEach((k, operator) -> messageProducer(operator, msg));
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
            messageProducer(operator, msg);
        }
    }

    public void publishStatus(String server, TaskStatus status) {
        //发布状态变化
        String msg = formatMsg(server, WsEventEnum.SERVER_STATUS, status.name());
        this.sessionMap.forEach((k, session) -> messageProducer(session, msg));
    }

    public void commandEnd(String server, String body) {
        String msg = formatMsg(server, WsEventEnum.CMD_END, body);
        this.sessionMap.forEach((k, session) -> messageProducer(session, msg));
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
            messageProducer(operator, msg);
        }
    }

    public void notice(String text, NoticeEnum level) {
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
            this.sessionMap.forEach((k, session) -> messageProducer(session, msg));
        }
    }

    private static String formatMsg(String server, WsEventEnum event, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(server).append(CommonConst.PROTOCOL_SPLIT)
                .append(event.ordinal()).append(CommonConst.PROTOCOL_SPLIT).append(body);
        return sb.toString();
    }

    public static void messageProducer(final MessageQueueOperator operator, String msg) {
        operator.newMessage(msg);
        if (operator.isRunning()) {
            return;
        }

        // 这里保证一个MessageQueueOperator只会有一个线程在消费
        synchronized (operator) { // NOSONAR
            // 若未开始，则启动消息处理线程进行消费
            operator.setRunning();
            instance.msgConsumerExecutor.execute(operator);
        }
    }
}
