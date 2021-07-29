package com.mz.jarboot.ws;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author majianzheng
 */
@ServerEndpoint("/public/jarboot-agent/ws/{server}")
@RestController
public class WebSocketAgentServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAgentServer.class);

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("server") String server) {
        logger.debug("{} Agent连接成功!", server);
        AgentManager.getInstance().online(server, session);
        WebSocketManager.getInstance().sendConsole(server, server + " connected!");
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session, @PathParam("server") String server) {
        logger.debug("目标进程断开连接, id:{}, server:{}", session.getId(), server);
        AgentManager.getInstance().offline(server);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message, Session session, @PathParam("server") String server) {
        onTextMessage(new String(message), session, server);
    }

    @OnMessage
    public void onTextMessage(String message, Session session, @PathParam("server") String server) {
        CommandResponse resp = CommandResponse.createFromRaw(message);
        AgentManager.getInstance().handleAgentResponse(server, resp);
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session, Throwable error, @PathParam("server") String server) {
        onClose(session, server);
    }
}
