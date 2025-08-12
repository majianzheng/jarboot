package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.common.protocol.CommandResponse;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import javax.websocket.Session;

/**
 * @author majianzheng
 */
public class AgentResponseEvent implements JarbootEvent {
    private String userDir;
    private final String serviceName;
    private final String sid;
    private final CommandResponse response;
    private final transient Session session;

    public AgentResponseEvent(String userDir, String serviceName, String sid, CommandResponse response, Session session) {
        this.userDir = StringUtils.isEmpty(userDir) ? "default" : userDir;
        this.serviceName = serviceName;
        this.sid = sid;
        this.response = response;
        this.session = session;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getSid() {
        return this.sid;
    }

    public CommandResponse getResponse() {
        return this.response;
    }

    public Session getSession() {
        return this.session;
    }

    public String getUserDir() {
        return userDir;
    }

    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }
}
