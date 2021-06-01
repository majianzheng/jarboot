package com.mz.jarboot.ws;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.base.AgentManager;
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
    private static final int CMD_FUNC = 1;
    private static final int CANCEL_FUNC = 2;

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
        JSONObject json = JSON.parseObject(message);
        String server = json.getString("server");
        int func = json.getIntValue("func");
        String body = json.getString("body");
        logger.info("server:{}, msgType:{}, msgBody:{}", server, func, body);
        switch (func) {
            case CMD_FUNC:
                AgentManager.getInstance().sendCommand(server, body);
                break;
            case CANCEL_FUNC:
                AgentManager.getInstance().sendInternalCommand(server, "cancel");
                break;
            default:
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
        WebSocketManager.getInstance().delConnect(session.getId());
        logger.error(error.getMessage(), error);
    }
}
