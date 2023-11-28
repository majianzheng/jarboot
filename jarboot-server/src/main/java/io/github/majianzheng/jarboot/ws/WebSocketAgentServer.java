package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.protocol.CommandResponse;
import io.github.majianzheng.jarboot.event.AgentResponseEvent;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author majianzheng
 */
@ServerEndpoint("/jarboot/public/agent/ws/{service}/{sid}/{userDir}")
@RestController
public class WebSocketAgentServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAgentServer.class);

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session,
                       @PathParam("service") String serviceName,
                       @PathParam("userDir") String userDir,
                       @PathParam("sid") String sid) {
        logger.debug("{} / {} @ {} Agent连接成功!", userDir, serviceName, sid);
        AgentManager.getInstance().online(userDir, serviceName, session, sid);
        String msg = String.format("\033[1;96m%s\033[0m connected!", serviceName);
        MessageUtils.console(sid, msg);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session,
                         @PathParam("service") String serviceName,
                         @PathParam("sid") String sid) {
        logger.debug("目标进程断开连接, id:{}, serviceName:{}, sid:{}", session.getId(), serviceName, sid);
        AgentManager.getInstance().offline(sid);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message,
                                Session session,
                                @PathParam("service") String serviceName,
                                @PathParam("userDir") String userDir,
                                @PathParam("sid") String sid) {
        CommandResponse resp = CommandResponse.createFromRaw(message);
        NotifyReactor
                .getInstance()
                .publishEvent(new AgentResponseEvent(userDir, serviceName, sid, resp, session));
    }

    /**
     * 连接异常
     * @param error 错误
     */
    @OnError
    public void onError(Throwable error,
                        @PathParam("service") String server,
                        @PathParam("sid") String sid) {
        logger.debug( "sid: {}, server:{} socket connection error!{}", sid, server, error.getMessage());
    }
}
