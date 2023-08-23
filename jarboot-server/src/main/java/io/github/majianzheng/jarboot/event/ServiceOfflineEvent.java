package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;

/**
 * @author majianzheng
 */
public class ServiceOfflineEvent implements JarbootEvent {
    private ServiceSetting setting;
    public ServiceOfflineEvent(ServiceSetting setting) {
        this.setting = setting;
    }

    public ServiceSetting getSetting() {
        return setting;
    }

    public void setSetting(ServiceSetting setting) {
        this.setting = setting;
    }
}
