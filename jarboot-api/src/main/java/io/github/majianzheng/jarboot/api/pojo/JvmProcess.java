package io.github.majianzheng.jarboot.api.pojo;

import java.util.List;

/**
 * @author majianzheng
 */
public class JvmProcess extends BaseInstanceNode {
    private String pid;
    private String fullName;
    private Boolean attached;
    private String remote;
    private Boolean trusted;
    private List<JvmProcess> children;

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

    public List<JvmProcess> getChildren() {
        return children;
    }

    public void setChildren(List<JvmProcess> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "JvmProcess{" +
                "pid='" + pid + '\'' +
                ", fullName='" + fullName + '\'' +
                ", attached=" + attached +
                ", remote='" + remote + '\'' +
                ", trusted=" + trusted +
                ", children=" + children +
                '}';
    }
}
