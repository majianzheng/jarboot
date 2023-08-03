package com.mz.jarboot.event;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.pojo.ServiceSetting;

/**
 * @author majianzheng
 */
public class ServiceOnlineEvent implements JarbootEvent {
    private ServiceSetting setting;
    public ServiceOnlineEvent(ServiceSetting setting) {
        this.setting = setting;
    }

    public ServiceSetting getSetting() {
        return setting;
    }

    public void setSetting(ServiceSetting setting) {
        this.setting = setting;
    }
}
