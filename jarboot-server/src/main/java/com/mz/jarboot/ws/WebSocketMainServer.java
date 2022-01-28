package com.mz.jarboot.ws;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.event.BroadcastMessageEvent;
import com.mz.jarboot.event.FuncReceivedEvent;
import com.mz.jarboot.event.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向浏览器推送消息
 * @author majianzheng
 */
@ServerEndpoint(CommonConst.MAIN_WS_CONTEXT)
@RestController
public class WebSocketMainServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMainServer.class);
    private static final ConcurrentHashMap<String, SessionOperator> SESSIONS = new ConcurrentHashMap<>(32);
    /** 心跳ping */
    private static final String PING = "ping";

    static {
        register();
    }

    /**
     * 连接建立成功调用的方法
     * */
    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.put(session.getId(), new SessionOperator(session));
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session) {
        NotifyReactor
                .getInstance()
                .publishEvent(new FuncReceivedEvent(
                        FuncReceivedEvent.FuncCode.SESSION_CLOSED_FUNC, session.getId()));
        SESSIONS.remove(session.getId());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onTextMessage(String message, Session session) {
        if (StringUtils.isEmpty(message) || PING.equals(message)) {
            return;
        }
        FuncReceivedEvent event = JsonUtils.readValue(message, FuncReceivedEvent.class);
        if (null == event) {
            logger.error("解析json失败！{}", message);
            return;
        }
        event.setSessionId(session.getId());
        NotifyReactor.getInstance().publishEvent(event);
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.debug(error.getMessage(), error);
        this.onClose(session);
    }

    private static void register() {
        //定向推送
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<MessageEvent>() {
            @Override
            public void onEvent(MessageEvent event) {
                SessionOperator operator = SESSIONS.getOrDefault(event.getSessionId(), null);
                if (null != operator) {
                    operator.newMessage(event);
                }
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return MessageEvent.class;
            }
        });
        //广播推送
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<BroadcastMessageEvent>() {
            @Override
            public void onEvent(BroadcastMessageEvent event) {
                SESSIONS.values().forEach(operator -> operator.newMessage(event));
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return BroadcastMessageEvent.class;
            }
        });
    }
}
