package com.mz.jarboot.dto;

import java.io.Serializable;

public class GlobalSettingDTO implements Serializable {
    private String servicesPath;
    private String arthasHome;
    private String defaultJvmArg = "";
    private int maxStartTime = 30000;

    public String getServicesPath() {
        return servicesPath;
    }

    public void setServicesPath(String servicesPath) {
        this.servicesPath = servicesPath;
    }

    public String getArthasHome() {
        return arthasHome;
    }

    public void setArthasHome(String arthasHome) {
        this.arthasHome = arthasHome;
    }

    public String getDefaultJvmArg() {
        return defaultJvmArg;
    }

    public void setDefaultJvmArg(String defaultJvmArg) {
        this.defaultJvmArg = defaultJvmArg;
    }

    public int getMaxStartTime() {
        return maxStartTime;
    }

    public void setMaxStartTime(int maxStartTime) {
        this.maxStartTime = maxStartTime;
    }
}
