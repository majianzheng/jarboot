package com.mz.jarboot.task;

public class TaskRunInfo {
    private String name;
    private Integer pid;
    private String status;
    private Long lastUpdateTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public Long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Long lastStartTime) {
        this.lastUpdateTime = lastStartTime;
    }
}
