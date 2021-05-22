package com.mz.jarboot.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/jarboot-service/ws")
@RestController
public class WebSocketMainServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMainServer.class);

//    @Autowired
//    private ServerMgrService serverMgrService;

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session) {
        WebSocketManager.getInstance().addNewConnect(session);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session) {
        WebSocketManager.getInstance().delConnect(session.getId());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message, Session session) {
        // Do nothing
        logger.info("手动二进制消息：{}", new String(message));
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
        logger.info("手动文本消息：{}", message);
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        WebSocketManager.getInstance().delConnect(session.getId());
        logger.error(error.getMessage(), error);
    }
}
