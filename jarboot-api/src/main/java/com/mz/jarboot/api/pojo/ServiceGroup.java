package com.mz.jarboot.api.pojo;

import java.util.List;

/**
 * @author mazheng
 */
public class ServiceGroup extends AbstractInstance {
    private String host;

    private Boolean onlineDebug;
    private List<AbstractInstance> children;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public List<AbstractInstance> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<AbstractInstance> children) {
        this.children = children;
    }

    public Boolean getOnlineDebug() {
        return onlineDebug;
    }

    public void setOnlineDebug(Boolean onlineDebug) {
        this.onlineDebug = onlineDebug;
    }

    @Override
    public String toString() {
        return "ServiceGroup{" +
                "host='" + host + '\'' +
                ", name='" + getName() + '\'' +
                ", children=" + children +
                '}';
    }
}
