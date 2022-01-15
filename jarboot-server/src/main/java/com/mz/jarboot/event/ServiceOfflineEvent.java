package com.mz.jarboot.event;

import com.mz.jarboot.api.event.JarbootEvent;

/**
 * @author majianzheng
 */
public class ServiceOfflineEvent implements JarbootEvent {
    private String serviceName;
    private String sid;

    public ServiceOfflineEvent(String serviceName, String sid) {
        this.serviceName = serviceName;
        this.sid = sid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }
}
