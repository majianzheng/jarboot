package com.mz.jarboot.ws;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.event.AbstractMessageEvent;

import javax.websocket.Session;
import java.io.IOException;

/**
 * WebSocket消息推送
 * @author majianzheng
 */
public class SessionOperator {
    /** websocket会话 */
    private final Session session;
    
    public SessionOperator(Session session) {
        this.session = session;
    }

    /**
     * 新消息投递
     * @param msg 消息
     */
    public void newMessage(String msg) {
        publish(new MessageSenderEvent(session, msg));
    }

    /**
     * 新消息投递
     * @param msg 消息
     */
    public void newMessage(byte[] msg) {
        publish(new MessageSenderEvent(session, msg));
    }

    /**
     * 新消息投递
     * @param msg 消息
     */
    public void newMessage(AbstractMessageEvent msg) {
        newMessage(msg.message());
    }

    /**
     * 检查会话是否存活
     * @return 是否存活
     */
    public boolean isOpen() {
        return this.session.isOpen();
    }

    /**
     * 关闭
     */
    public void close() {
        try {
            this.session.close();
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    private void publish(MessageSenderEvent event) {
        NotifyReactor.getInstance().publishEvent(event);
    }
}
