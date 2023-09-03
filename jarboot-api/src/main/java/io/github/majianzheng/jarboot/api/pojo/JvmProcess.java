package io.github.majianzheng.jarboot.api.pojo;

import java.util.List;

/**
 * @author majianzheng
 */
public class JvmProcess {
    private String pid;
    private String fullName;
    private Boolean attached;
    private String remote;
    private Boolean trusted;
    /** 集群模式下要指定，非集群忽略 */
    private String host;
    private String sid;
    private String name;
    private List<JvmProcess> children;
    private int nodeType;
    private String status;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getAttached() {
        return attached;
    }

    public void setAttached(Boolean attached) {
        this.attached = attached;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public Boolean getTrusted() {
        return trusted;
    }

    public void setTrusted(Boolean trusted) {
        this.trusted = trusted;
    }

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

    public List<JvmProcess> getChildren() {
        return children;
    }

    public void setChildren(List<JvmProcess> children) {
        this.children = children;
    }

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "JvmProcess{" +
                "pid='" + pid + '\'' +
                ", fullName='" + fullName + '\'' +
                ", attached=" + attached +
                ", remote='" + remote + '\'' +
                ", trusted=" + trusted +
                ", host='" + host + '\'' +
                ", sid='" + sid + '\'' +
                ", name='" + name + '\'' +
                ", children=" + children +
                ", nodeType=" + nodeType +
                '}';
    }
}
