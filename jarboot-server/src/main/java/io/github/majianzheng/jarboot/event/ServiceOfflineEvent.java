package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;

/**
 * @author majianzheng
 */
public class ServiceOfflineEvent implements JarbootEvent {
    private ServiceSetting setting;
    private final boolean stopping;
    private final boolean starting;
    public ServiceOfflineEvent(ServiceSetting setting, boolean stopping, boolean starting) {
        this.setting = setting;
        this.stopping = stopping;
        this.starting = starting;
    }

    public ServiceSetting getSetting() {
        return setting;
    }

    public void setSetting(ServiceSetting setting) {
        this.setting = setting;
    }

    public boolean isStopping() {
        return stopping;
    }
    public boolean isStarting() {
        return starting;
    }
}
