package com.mz.jarboot.api.pojo;

import java.util.Objects;

/**
 * @author majianzheng
 */
public class JvmProcess extends AbstractInstance {
    private String pid;
    private String fullName;
    private Boolean attached;
    private String remote;
    private Boolean trusted;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JvmProcess that = (JvmProcess) o;
        return Objects.equals(getSid(), that.getSid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSid());
    }

    @Override
    public String toString() {
        return "JvmProcess{" +
                "sid='" + getSid() + '\'' +
                ", pid='" + pid + '\'' +
                ", name='" + getSid() + '\'' +
                ", fullName='" + fullName + '\'' +
                ", attached=" + attached +
                ", remote='" + remote + '\'' +
                ", trusted=" + trusted +
                '}';
    }
}
