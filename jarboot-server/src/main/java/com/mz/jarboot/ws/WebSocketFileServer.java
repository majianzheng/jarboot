package com.mz.jarboot.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author majianzheng
 */
@ServerEndpoint("/jarboot/file/ws/{file}")
@RestController
public class WebSocketFileServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketFileServer.class);

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("file") String file) {
        logger.debug("{}开始上传", file);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose( Session session, @PathParam("file") String file) {
        logger.debug("{}上传完成", file);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onBinaryMessage(byte[] message, Session session,@PathParam("file") String file) {
        // todo
    }

    /**
     * 连接异常
     * @param session 会话
     * @param error 错误
     */
    @OnError
    public void onError(Session session,
                        Throwable error,
                        @PathParam("file") String file) {
        logger.debug( "{} socket connection error!{}", file, error.getMessage(), error);
        onClose(session, file);
    }
}
