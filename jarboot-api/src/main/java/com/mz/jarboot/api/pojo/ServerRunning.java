package com.mz.jarboot.api.pojo;

/**
 * @author majianzheng
 */
public class ServerRunning {
    private Integer pid;
    private String status;
    private String name;
    /** 是否临时的进程，实际存在，在工作空间中不存在 */
    private Boolean ephemeral;

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(Boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    @Override
    public String toString() {
        return "ServerRunning{" +
                "pid=" + pid +
                ", status='" + status + '\'' +
                ", name='" + name + '\'' +
                ", debug=" + ephemeral +
                '}';
    }
}
