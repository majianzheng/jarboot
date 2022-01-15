package com.mz.jarboot.ws;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.protocol.CommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author majianzheng
 */
@ServerEndpoint("/jarboot/public/agent/ws/{service}/{sid}")
@RestController
public class WebSocketAgentServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAgentServer.class);

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session,
                       @PathParam("service") String serviceName,
                       @PathParam("sid") String sid) {
        logger.debug("{} @ {} Agent连接成功!", serviceName, sid);
        AgentManager.getInstance().online(serviceName, session, sid);
        String msg = String.format("\033[1;96m%s\033[0m connected!", serviceName);
        WebSocketManager.getInstance().sendConsole(sid, msg);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session,
                         @PathParam("service") String serviceName,
                         @PathParam("sid") String sid) {
        logger.debug("目标进程断开连接, id:{}, serviceName:{}, sid:{}", session.getId(), serviceName, sid);
        AgentManager.getInstance().offline(serviceName, sid);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message,
                                Session session,
                                @PathParam("service") String serviceName,
                                @PathParam("sid") String sid) {
        CommandResponse resp = CommandResponse.createFromRaw(message);
        AgentManager.getInstance().handleAgentResponse(serviceName, sid, resp, session);
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session,
                        Throwable error,
                        @PathParam("service") String server,
                        @PathParam("sid") String sid) {
        logger.debug( "{} socket connection error!{}", server, error.getMessage());
        onClose(session, server, sid);
    }
}
