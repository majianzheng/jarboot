package io.github.majianzheng.jarboot.api.pojo;

import java.io.Serializable;

/**
 * @author majianzheng
 */
public class SystemSetting implements Serializable {
    private String workspace;
    private String jdkPath;
    private String defaultVmOptions;
    private Boolean servicesAutoStart;
    private Integer maxStartTime;
    private Integer maxExitTime;
    private String afterServerOfflineExec;
    private Integer fileChangeShakeTime;

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getJdkPath() {
        return jdkPath;
    }

    public void setJdkPath(String jdkPath) {
        this.jdkPath = jdkPath;
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

    public Integer getMaxStartTime() {
        return maxStartTime;
    }

    public void setMaxStartTime(Integer maxStartTime) {
        this.maxStartTime = maxStartTime;
    }

    public Integer getMaxExitTime() {
        return maxExitTime;
    }

    public void setMaxExitTime(Integer maxExitTime) {
        this.maxExitTime = maxExitTime;
    }

    public String getAfterServerOfflineExec() {
        return afterServerOfflineExec;
    }

    public void setAfterServerOfflineExec(String afterServerOfflineExec) {
        this.afterServerOfflineExec = afterServerOfflineExec;
    }

    public Integer getFileChangeShakeTime() {
        return fileChangeShakeTime;
    }

    public void setFileChangeShakeTime(Integer fileChangeShakeTime) {
        this.fileChangeShakeTime = fileChangeShakeTime;
    }
}
