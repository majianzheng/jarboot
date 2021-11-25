package com.mz.jarboot.api.pojo;

import java.util.Objects;

/**
 * @author majianzheng
 */
public class JvmProcess {
    private String sid;
    private int pid;
    private String name;
    private String fullName;
    private Boolean attached;
    private String remote;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JvmProcess that = (JvmProcess) o;
        return Objects.equals(sid, that.sid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sid);
    }

    @Override
    public String toString() {
        return "JvmProcess{" +
                "sid='" + sid + '\'' +
                ", pid=" + pid +
                ", name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", attached=" + attached +
                ", remote='" + remote + '\'' +
                '}';
    }
}
