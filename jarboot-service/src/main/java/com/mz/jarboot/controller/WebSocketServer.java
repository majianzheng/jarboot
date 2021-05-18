package com.mz.jarboot.controller;

import com.mz.jarboot.ws.WebSocketConnManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/jarboot-service/ws")
@RestController
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

//    @Autowired
//    private ServerMgrService serverMgrService;

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session) {
        WebSocketConnManager.getInstance().addNewConnect(session);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session) {
        WebSocketConnManager.getInstance().delConnect(session.getId());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message, Session session) {
        // Do nothing because of X and Y.
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
//        MessageRequestDTO request = JSON.parseObject(message, MessageRequestDTO.class);
//        switch (request.getMethod()) {
//            case "oneClickRestart":
//                serverMgrService.oneClickRestart();
//                break;
//            case "oneClickStart":
//                serverMgrService.oneClickStart();
//                break;
//            case "oneClickStop":
//                serverMgrService.oneClickStop();
//                break;
//            case "initSystem":
//                serverMgrService.initSystem();
//                break;
//            default:
//                break;
//        }
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        WebSocketConnManager.getInstance().delConnect(session.getId());
        logger.error(error.getMessage(), error);
    }
}
