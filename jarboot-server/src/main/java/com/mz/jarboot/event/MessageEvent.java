package com.mz.jarboot.event;

import com.mz.jarboot.common.protocol.NotifyType;

/**
 * @author majianzheng
 */
public class MessageEvent extends AbstractMessageEvent {
    private final String sessionId;

    public MessageEvent(String sid, String sessionId) {
        this.sid = sid;
        this.sessionId = sessionId;
    }

    public MessageEvent body(String body) {
        this.body = body;
        return this;
    }

    public MessageEvent body(String text, NotifyType level) {
        this.noticeBody(text, level);
        return this;
    }

    public MessageEvent type(FrontEndNotifyEventType type) {
        this.type = type;
        return this;
    }

    public String getSessionId() {
        return this.sessionId;
    }
}
