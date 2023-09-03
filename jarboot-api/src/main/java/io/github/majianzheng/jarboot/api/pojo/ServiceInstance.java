package io.github.majianzheng.jarboot.api.pojo;

import java.util.List;

/**
 * @author majianzheng
 */
public class ServiceInstance {
    /** 集群模式下要指定，非集群忽略 */
    private String host;
    private String sid;
    private String name;
    private String status;
    private String group;
    private int nodeType;

    private List<ServiceInstance> children;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "host='" + host + '\'' +
                ", sid='" + sid + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", group='" + group + '\'' +
                ", nodeType=" + nodeType +
                ", children=" + children +
                '}';
    }
}
