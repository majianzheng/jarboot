package com.mz.jarboot.ws;

import com.alibaba.fastjson.JSON;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/jarboot-agent/ws")
@RestController
public class WebSocketAgentServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAgentServer.class);
    private static final Map<String, String> sessionIdToServer = new ConcurrentHashMap<>(64);
    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session) {
        //do nothing
        logger.info("Agent连接成功！");
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session) {
        String server = sessionIdToServer.getOrDefault(session.getId(), null);
        if (null != server) {
            logger.info("目标进程断开连接, {}, server:{}", session.getId(), server);
            sessionIdToServer.remove(session.getId());
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
        CommandResponse resp = JSON.parseObject(message, CommandResponse.class);
        String type = resp.getType();
        String body = resp.getBody();
        logger.info(message);
        switch (type) {
            case CommandConst.ONLINE_TYPE:
                sessionIdToServer.put(session.getId(), body);
                AgentManager.getInstance().online(body, session);
                break;
            case CommandConst.ACK_TYPE:
                String server = sessionIdToServer.getOrDefault(session.getId(), null);
                if (null != server) {
                    AgentManager.getInstance().onAck(server, resp);
                }
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
