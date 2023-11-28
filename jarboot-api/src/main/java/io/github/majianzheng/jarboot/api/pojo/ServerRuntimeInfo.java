package io.github.majianzheng.jarboot.api.pojo;

/**
 * @author mazheng
 */
public class ServerRuntimeInfo {
    private String machineCode;
    private String host;
    private String uuid;
    private String version;
    private String workspace;
    private Boolean inDocker;

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
}
