package com.mz.jarboot.api.pojo;

import java.io.Serializable;

/**
 * @author majianzheng
 */
public class GlobalSetting implements Serializable {
    private String servicesPath;
    private String defaultVmOptions;
    private Boolean servicesAutoStart;

    public String getServicesPath() {
        return servicesPath;
    }

    public void setServicesPath(String servicesPath) {
        this.servicesPath = servicesPath;
    }

    public String getDefaultVmOptions() {
        return defaultVmOptions;
    }

    public void setDefaultVmOptions(String defaultVmOptions) {
        this.defaultVmOptions = defaultVmOptions;
    }

    public Boolean getServicesAutoStart() {
        return servicesAutoStart;
    }

    public void setServicesAutoStart(Boolean servicesAutoStart) {
        this.servicesAutoStart = servicesAutoStart;
    }

    @Override
    public String toString() {
        return "GlobalSettingDTO{" +
                "servicesPath='" + servicesPath + '\'' +
                ", defaultVmOptions='" + defaultVmOptions + '\'' +
                ", servicesAutoStart=" + servicesAutoStart +
                '}';
    }
}
