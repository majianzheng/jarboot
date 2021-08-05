package com.mz.jarboot.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;

/**
 * 消息发送
 * @author jianzhengma
 */
public class MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private final Session session;
    private final String message;
    public MessageSender(Session session, String message) {
        this.session = session;
        this.message = message;
    }

    public void sendText() {
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
