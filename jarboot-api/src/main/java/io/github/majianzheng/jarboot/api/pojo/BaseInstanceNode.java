package io.github.majianzheng.jarboot.api.pojo;

/**
 * 实例Node
 * @author mazheng
 */
public abstract class BaseInstanceNode {
    /** 集群模式下要指定，非集群忽略 */
    protected String host;
    protected String hostName;
    protected int nodeType;
    protected String name;
    protected String sid;
    protected String status;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
