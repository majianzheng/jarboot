package com.mz.jarboot.api.pojo;

import java.util.List;

/**
 * @author mazheng
 */
public class ServiceGroup extends ServiceInstance {
    private String host;
    private List<ServiceGroup> children;

    public static ServiceGroup wrapGroup(ServiceInstance instance) {
        ServiceGroup group = new ServiceGroup();
        group.setName(instance.getName());
        group.setStatus(instance.getStatus());
        group.setPath(instance.getPath());
        group.setSid(instance.getSid());
        return group;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<ServiceGroup> getChildren() {
        return children;
    }

    public void setChildren(List<ServiceGroup> children) {
        this.children = children;
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
