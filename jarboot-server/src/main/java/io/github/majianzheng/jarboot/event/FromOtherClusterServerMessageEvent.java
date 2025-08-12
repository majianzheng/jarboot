package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.ClusterEvent;

/**
 * @author mazheng
 */
public class FromOtherClusterServerMessageEvent extends ClusterEvent {
    private String sid;
    private String sessionId;
    private String message;
    public FromOtherClusterServerMessageEvent(String sid, String sessionId, String message) {
        this.sid = sid;
        this.sessionId = sessionId;
        this.message = message;
    }
    public FromOtherClusterServerMessageEvent() {
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
