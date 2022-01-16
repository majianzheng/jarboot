package com.mz.jarboot.event;

import com.mz.jarboot.api.event.JarbootEvent;

/**
 * @author jianzhengma
 */
public class ServiceStartedEvent implements JarbootEvent {
    private final String serviceName;
    private final String sid;
    public ServiceStartedEvent(String serviceName, String sid) {
        this.serviceName = serviceName;
        this.sid = sid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getSid() {
        return sid;
    }
}
