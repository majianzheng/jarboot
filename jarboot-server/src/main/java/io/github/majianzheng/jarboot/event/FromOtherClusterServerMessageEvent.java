package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;

/**
 * @author mazheng
 */
public class FromOtherClusterServerMessageEvent implements JarbootEvent {
    private String sessionId;
    private String message;

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
