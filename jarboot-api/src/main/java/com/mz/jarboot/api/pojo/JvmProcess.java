package com.mz.jarboot.api.pojo;

/**
 * @author majianzheng
 */
public class JvmProcess {
    private int pid;
    private String name;
    private String fullName;
    private Boolean attached;

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

    @Override
    public String toString() {
        return "JvmProcess{" +
                "pid=" + pid +
                ", name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", attached=" + attached +
                '}';
    }
}
