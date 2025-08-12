package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.config.WsConfigurator;
import io.github.majianzheng.jarboot.dao.UserDao;
import io.github.majianzheng.jarboot.event.FromOtherClusterServerMessageEvent;
import io.github.majianzheng.jarboot.common.notify.DefaultPublisher;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.event.BroadcastMessageEvent;
import io.github.majianzheng.jarboot.event.FuncReceivedEvent;
import io.github.majianzheng.jarboot.event.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向浏览器推送消息
 * @author majianzheng
 */
@ServerEndpoint(value = CommonConst.MAIN_WS_CONTEXT, configurator = WsConfigurator.class)
@RestController
@SuppressWarnings({"java:S2696"})
public class WebSocketMainServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMainServer.class);
    private static final ConcurrentHashMap<String, SessionOperator> SESSIONS = new ConcurrentHashMap<>(32);
    /** 推送前端消息 */
    public static final DefaultPublisher PUBLISHER = new DefaultPublisher(32768, "fe.push.publisher");
    private static UserDao userDao;

    static {
        register();
    }

    @Autowired
    public void setUserDao(UserDao dao) {
        userDao = dao;
    }

    /**
     * 连接建立成功调用的方法
     * */
    @OnOpen
    public void onOpen(Session session) {
        String username = session.getUserPrincipal().getName();
        String userDir = userDao.getUserDirByName(username);
        SESSIONS.put(session.getId(), new SessionOperator(userDir, session));
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session) {
        FuncReceivedEvent event = new FuncReceivedEvent(
                FuncReceivedEvent.FuncCode.SESSION_CLOSED_FUNC, session.getId());
        NotifyReactor
                .getInstance()
                .publishEvent(event);
        SESSIONS.remove(session.getId());
        ClusterClientManager.getInstance().execClusterFunc(event);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onTextMessage(String message, Session session) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        if (CommonConst.PING.equals(message)) {
            NotifyReactor
                    .getInstance()
                    .publishEvent(new MessageSenderEvent(session, CommonConst.PING));
            return;
        }
        FuncReceivedEvent event = JsonUtils.readValue(message, FuncReceivedEvent.class);
        if (null == event) {
            logger.error("解析json失败！{}", message);
            return;
        }
        event.setSessionId(session.getId());
        String self = ClusterClientManager.getInstance().getSelfHost();
        if (StringUtils.isEmpty(self) || Objects.equals(self, event.getHost())) {
            NotifyReactor.getInstance().publishEvent(event);
        } else {
            ClusterClientManager.getInstance().execClusterFunc(event);
        }
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
                String sessionId = event.getSessionId();
                int index = sessionId.indexOf(StringUtils.SPACE);
                if (index > 0) {
                    String host = sessionId.substring(0, index);
                    sessionId = sessionId.substring(index + 1);
                    handleClusterEvent(event, sessionId, host);
                } else {
                    SessionOperator operator = SESSIONS.getOrDefault(event.getSessionId(), null);
                    if (null != operator) {
                        operator.newMessage(event);
                    }
                }
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return MessageEvent.class;
            }
        }, PUBLISHER);
        //广播推送
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<BroadcastMessageEvent>() {
            @Override
            public void onEvent(BroadcastMessageEvent event) {
                if (event.getSessionIds().isEmpty()) {
                    SESSIONS.values().forEach(operator -> operator.newMessage(event));
                    FromOtherClusterServerMessageEvent messageEvent = new FromOtherClusterServerMessageEvent();
                    messageEvent.setMessage(event.message());
                    ClusterClientManager.getInstance().notifyToOtherCluster(messageEvent);
                    return;
                }
                // 定点广播
                event.getSessionIds().forEach(sessionId -> broadToDirect(event, sessionId));
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return BroadcastMessageEvent.class;
            }
        }, PUBLISHER);

        // 来自其他集群的推送
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<FromOtherClusterServerMessageEvent>() {
            @Override
            public void onEvent(FromOtherClusterServerMessageEvent event) {
                if (StringUtils.isEmpty(event.getSessionId())) {
                    SESSIONS.values().forEach(operator -> operator.newMessage(event.getMessage()));
                } else {
                    SessionOperator operator = SESSIONS.getOrDefault(event.getSessionId(), null);
                    if (null != operator) {
                        operator.newMessage(event.getMessage());
                    }
                }
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return FromOtherClusterServerMessageEvent.class;
            }
        }, PUBLISHER);
    }

    private static void handleClusterEvent(MessageEvent event, String sessionId, String host) {
        if (Objects.equals(ClusterClientManager.getInstance().getSelfHost(), host)) {
            SessionOperator operator = SESSIONS.getOrDefault(sessionId, null);
            if (null != operator) {
                operator.newMessage(event);
            }
            return;
        }
        ClusterClientManager.getInstance().notifyToOtherCluster(host, new FromOtherClusterServerMessageEvent(event.getSid(), sessionId, event.message()));
    }

    private static void broadToDirect(BroadcastMessageEvent event, String sessionId) {
        int index = sessionId.indexOf(StringUtils.SPACE);
        if (index > 0) {
            String host = sessionId.substring(0, index);
            sessionId = sessionId.substring(index + 1);
            if (Objects.equals(ClusterClientManager.getInstance().getSelfHost(), host)) {
                // 连接的当前节点
                SessionOperator operator = SESSIONS.getOrDefault(sessionId, null);
                if (null != operator) {
                    operator.newMessage(event);
                }
                return;
            }
            ClusterClientManager.getInstance().notifyToOtherCluster(host, new FromOtherClusterServerMessageEvent(event.getSid(), sessionId, event.message()));
        } else {
            SessionOperator operator = SESSIONS.getOrDefault(sessionId, null);
            if (null != operator) {
                operator.newMessage(event);
            }
        }
    }
}
