package com.mz.jarboot.ws;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.List;

@ServerEndpoint("/jarboot-agent/ws")
@RestController
public class WebSocketAgentServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAgentServer.class);
    private static final String SERVER_PARAM_KEY = "server";

    private String getServer(Session session) {
        List<String> params = session.getRequestParameterMap().getOrDefault(SERVER_PARAM_KEY, null);
        if (null == params || params.isEmpty()) {
            return null;
        }
        return params.get(0);
    }
    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session) {
        String server = getServer(session);
        logger.info("客户端{} Agent连接成功!", server);
        if (null != server) {
            AgentManager.getInstance().online(server, session);
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session) {
        String server = getServer(session);
        if (null != server) {
            logger.info("目标进程断开连接, {}, server:{}", session.getId(), server);
            AgentManager.getInstance().offline(server);
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message, Session session) {
        onTextMessage(new String(message), session);
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
        logger.info("agent msg:{}", message);
        String server = getServer(session);
        if (null == server) {
            logger.warn("server is null.");
            return;
        }
        CommandResponse resp = CommandResponse.createFromRaw(message);
        ResponseType type = resp.getResponseType();
        logger.info("type:{}", type);
        switch (type) {
            case ACK:
                AgentManager.getInstance().onAck(server, resp);
                break;
            case CONSOLE:
                WebSocketManager.getInstance().sendOutMessage(server, resp.getBody());
                break;
            case COMPLETE:
                if (Boolean.FALSE.equals(resp.getSuccess())) {
                    WebSocketManager.getInstance().sendOutMessage(server, resp.getBody());
                }
                WebSocketManager.getInstance().commandComplete(server, resp.getCmd());
                break;
            default:
                //do nothing
                break;
        }
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        onClose(session);
        logger.error(error.getMessage(), error);
    }
}
