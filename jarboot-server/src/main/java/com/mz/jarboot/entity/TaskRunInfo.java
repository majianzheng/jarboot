package com.mz.jarboot.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Table(name = TaskRunInfo.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
@Entity
public class TaskRunInfo extends AbstractBaseEntity {
    public static final String TABLE_NAME = "jarboot_task_run_info";
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
