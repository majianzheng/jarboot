package com.mz.jarboot.ws;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.event.BroadcastMessageEvent;
import com.mz.jarboot.event.FuncReceivedEvent;
import com.mz.jarboot.event.MessageEvent;
import com.mz.jarboot.security.JwtTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向浏览器推送消息
 * @author majianzheng
 */
@ServerEndpoint("/jarboot/public/service/ws")
@RestController
public class WebSocketMainServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMainServer.class);

    /** 心跳ping */
    private static final String PING = "ping";

    /**
     * 连接建立成功调用的方法
     * */
    @OnOpen
    public void onOpen(Session session) {
        if (validateToken(session)) {
            Holder.SESSIONS.put(session.getId(), new SessionOperator(session));
        }
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
        Holder.SESSIONS.remove(session.getId());
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

    public static boolean validateToken(Session session) {
        if (!Holder.JWT_MGR.getEnabled()) {
            return true;
        }
        //获取token
        List<String> array = session.getRequestParameterMap().get("token");
        if (CollectionUtils.isEmpty(array)) {
            logger.error("WebSocket connect failed, need token!");
            try {
                session.getBasicRemote().sendText("Token is empty!");
                session.close();
            } catch (IOException exception) {
                logger.warn(exception.getMessage(), exception);
            }
            return false;
        }
        String token = array.get(0);
        //校验token合法性
        try {
            Holder.JWT_MGR.validateToken(token);
        } catch (Exception e) {
            logger.error("Validate token failed!\ntoken:{}", token, e);
            try {
                session.getBasicRemote().sendText("Validate token failed!");
                session.close();
            } catch (IOException exception) {
                logger.warn(exception.getMessage(), exception);
            }
            return false;
        }
        return true;
    }

    private static class Holder {
        static final JwtTokenManager JWT_MGR = ApplicationContextUtils.getContext().getBean(JwtTokenManager.class);
        static final ConcurrentHashMap<String, SessionOperator> SESSIONS = new ConcurrentHashMap<>(32);
        static {
            register();
        }
        private static void register() {
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
}
