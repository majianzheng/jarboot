package io.github.majianzheng.jarboot.api.pojo;

import io.github.majianzheng.jarboot.api.constant.ClusterServerState;

/**
 * @author majianzheng
 */
public class HostInfo {
    private String host;
    private String name;
    private ClusterServerState state;
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClusterServerState getState() {
        return state;
    }

    public void setState(ClusterServerState state) {
        this.state = state;
    }
}
