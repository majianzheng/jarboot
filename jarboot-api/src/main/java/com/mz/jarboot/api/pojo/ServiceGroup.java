package com.mz.jarboot.api.pojo;

import java.util.List;

/**
 * @author mazheng
 */
public class ServiceGroup extends SimpleInstance {
    private Boolean onlineDebug;
    private List<SimpleInstance> children;

    @Override
    public List<SimpleInstance> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<SimpleInstance> children) {
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
                ", name='" + getName() + '\'' +
                ", children=" + children +
                '}';
    }
}
