package com.mz.jarboot.ws;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.nio.ByteBuffer;

/**
 * 消息发送
 * @author jianzhengma
 */
public class MessageSenderEvent implements JarbootEvent {
    private static final Logger logger = LoggerFactory.getLogger(MessageSenderEvent.class);
    private final Session session;
    private final String message;
    private final byte[] buf;
    private final boolean binary;

    public MessageSenderEvent(Session session, String message) {
        this.session = session;
        this.message = message;
        this.buf = new byte[1];
        this.binary = false;
    }

    public MessageSenderEvent(Session session, byte[] message) {
        this.session = session;
        this.message = StringUtils.EMPTY;
        this.buf = message;
        this.binary = true;
    }

    public void send() {
        if (!session.isOpen()) {
            return;
        }
        if (this.binary) {
            sendBinary();
        } else {
            sendText();
        }
    }

    private void sendText() {
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    private void sendBinary() {
        try {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(this.buf));
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
