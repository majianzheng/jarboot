package com.mz.jarboot.ws;

import com.mz.jarboot.terminal.TerminalProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

/**
 * @author mazheng
 */
@ServerEndpoint("/jarboot/main/terminal/ws")
@RestController
public class TerminalServer {
    private static final Logger logger = LoggerFactory.getLogger(TerminalServer.class);
    private final TerminalProcess terminal = new TerminalProcess();
    @OnOpen
    public void onOpen(Session session) {
        terminal.init(session);
    }
    @OnClose
    public void onClose( Session session) {
        terminal.destroy();
    }
    @OnError
    public void onError(Session session, Throwable error) {
        logger.debug(error.getMessage(), error);
        this.onClose(session);
    }

    @OnMessage
    public void onTextMessage(String message, Session session) {
        terminal.exec(message);
    }
}
