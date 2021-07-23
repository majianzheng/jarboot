package com.mz.jarboot.dto;

/**
 * @author jianzhengma
 */
public class ServerRunningDTO {
    private Integer pid;
    private String status;
    private String name;

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

    @Override
    public String toString() {
        return "ServerRunningDTO{" +
                "pid=" + pid +
                ", status='" + status + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
