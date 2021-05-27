package com.mz.jarboot.ws;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResultCodeConst;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

/**
 * 向浏览器推送消息
 * @author majianzheng
 */
@ServerEndpoint("/jarboot-service/ws")
@RestController
public class WebSocketMainServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMainServer.class);

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
        //do nothing
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
        logger.info(message);
        if (StringUtils.isEmpty(message)) {
            return;
        }
        int p = message.indexOf(' ');
        if (-1 == p || p > (message.length() - 2)) {
            return;
        }
        String msgType = message.substring(0, p);
        String msgBody = message.substring(p + 1);
        logger.info("msgType:{}, msgBody:{}", msgType, msgBody);
        if ("cmd".equals(msgType)) {
            p = msgBody.indexOf(' ');
            if (-1 == p) {
                return;
            }
            String server = msgBody.substring(0, p);
            String command = msgBody.substring(p + 1);
            CommandResponse resp = AgentManager.getInstance().sendCommandSync(server, command);
            if (ResultCodeConst.SUCCESS != resp.getResultCode()) {
                WebSocketManager.sendMsg(session, server, "CMD_ACK", resp.getResultMsg());
            } else {
                WebSocketManager.sendMsg(session, server, "CMD_ACK", resp.getBody());
            }
        }
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
