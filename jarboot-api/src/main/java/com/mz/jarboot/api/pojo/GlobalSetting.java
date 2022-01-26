package com.mz.jarboot.api.pojo;

import java.io.Serializable;

/**
 * @author majianzheng
 */
public class GlobalSetting implements Serializable {
    private String workspace;
    private String defaultVmOptions;
    private Boolean servicesAutoStart;

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
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
        return "GlobalSetting{" +
                "workspace='" + workspace + '\'' +
                ", defaultVmOptions='" + defaultVmOptions + '\'' +
                ", servicesAutoStart=" + servicesAutoStart +
                '}';
    }
}
