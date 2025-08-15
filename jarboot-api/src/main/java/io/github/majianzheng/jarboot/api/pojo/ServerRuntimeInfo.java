package io.github.majianzheng.jarboot.api.pojo;

import lombok.ToString;

/**
 * @author mazheng
 */
@ToString
public class ServerRuntimeInfo {
    private String machineCode;
    private String host;
    private String uuid;
    private String version;
    private String workspace;
    private Boolean inDocker;
    private Boolean dev;
    private String jdk;
    private String os;
    private String pid;

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public Boolean getInDocker() {
        return inDocker;
    }

    public void setInDocker(Boolean inDocker) {
        this.inDocker = inDocker;
    }

    public Boolean getDev() {
        return dev;
    }

    public void setDev(Boolean dev) {
        this.dev = dev;
    }

    public String getJdk() {
        return jdk;
    }

    public void setJdk(String jdk) {
        this.jdk = jdk;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
