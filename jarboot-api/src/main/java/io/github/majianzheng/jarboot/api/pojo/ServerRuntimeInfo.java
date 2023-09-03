package io.github.majianzheng.jarboot.api.pojo;

/**
 * @author mazheng
 */
public class ServerRuntimeInfo {
    private String host;
    private String uuid;
    private String version;
    private Boolean inDocker;

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

    public Boolean getInDocker() {
        return inDocker;
    }

    public void setInDocker(Boolean inDocker) {
        this.inDocker = inDocker;
    }
}
