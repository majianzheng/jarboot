package io.github.majianzheng.jarboot.api.pojo;

import java.util.List;

/**
 * @author majianzheng
 */
public class ServiceInstance extends BaseInstanceNode {
    private String group;
    private List<ServiceInstance> children;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<ServiceInstance> getChildren() {
        return children;
    }

    public void setChildren(List<ServiceInstance> children) {
        this.children = children;
    }
}
