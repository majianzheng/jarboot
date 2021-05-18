package com.mz.jarboot.ws;

import javax.websocket.Session;
import java.nio.ByteBuffer;

public class MsgSendUtils {
    private final Session session;
    private MsgSendUtils(Session session) {
        this.session = session;
    }
    public static void sendText(Session session, String text) {
        new MsgSendUtils(session).send(text);
    }
    public static void sendBinary(Session session, byte [] data) {
        new MsgSendUtils(session).send(data);
    }
    private void send(String text) {
        synchronized (this.session) {
            try {
                session.getBasicRemote().sendText(text);
            } catch (Exception e) {
                //ignore
            }
        }
    }
    private void send(byte [] data) {
        synchronized (this.session) {
            try {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
