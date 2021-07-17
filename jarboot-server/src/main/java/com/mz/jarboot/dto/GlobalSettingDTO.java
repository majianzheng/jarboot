package com.mz.jarboot.dto;

import java.io.Serializable;

public class GlobalSettingDTO implements Serializable {
    private String servicesPath;
    private String defaultJvmArg = "";
    private Boolean servicesAutoStart;

    public String getServicesPath() {
        return servicesPath;
    }

    public void setServicesPath(String servicesPath) {
        this.servicesPath = servicesPath;
    }

    public String getDefaultJvmArg() {
        return defaultJvmArg;
    }

    public void setDefaultJvmArg(String defaultJvmArg) {
        this.defaultJvmArg = defaultJvmArg;
    }

    public Boolean getServicesAutoStart() {
        return servicesAutoStart;
    }

    public void setServicesAutoStart(Boolean servicesAutoStart) {
        this.servicesAutoStart = servicesAutoStart;
    }
}
