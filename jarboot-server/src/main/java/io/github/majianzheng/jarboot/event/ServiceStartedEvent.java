package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;

/**
 * @author jianzhengma
 */
public class ServiceStartedEvent implements JarbootEvent {
    private final String userDir;
    private final String serviceName;
    private final String sid;
    public ServiceStartedEvent(String userDir, String serviceName, String sid) {
        this.userDir = userDir;
        this.serviceName = serviceName;
        this.sid = sid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getSid() {
        return sid;
    }

    public String getUserDir() {
        return userDir;
    }
}
