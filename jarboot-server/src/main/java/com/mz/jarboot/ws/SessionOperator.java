package com.mz.jarboot.ws;

import com.mz.jarboot.common.notify.NotifyReactor;
import javax.websocket.Session;

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
        NotifyReactor.getInstance().publishEvent(new MessageSenderEvent(session, msg));
    }

    public void newMessage(byte[] msg) {
        NotifyReactor.getInstance().publishEvent(new MessageSenderEvent(session, msg));
    }

    /**
     * 检查会话是否存活
     * @return 是否存活
     */
    public boolean isOpen() {
        return this.session.isOpen();
    }
}
