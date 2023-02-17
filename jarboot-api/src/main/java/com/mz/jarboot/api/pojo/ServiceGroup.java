package com.mz.jarboot.api.pojo;

import java.util.List;

/**
 * @author mazheng
 */
public class ServiceGroup {
    private String name;
    private List<ServiceInstance> children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ServiceInstance> getChildren() {
        return children;
    }

    public void setChildren(List<ServiceInstance> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "ServiceGroup{" +
                "name='" + name + '\'' +
                ", children=" + children +
                '}';
    }
}
