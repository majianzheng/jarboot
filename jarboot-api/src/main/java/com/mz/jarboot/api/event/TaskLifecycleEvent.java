package com.mz.jarboot.api.event;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.TaskLifecycle;
import com.mz.jarboot.api.pojo.ServiceSetting;

/**
 * @author majianzheng
 */
public class TaskLifecycleEvent implements JarbootEvent {
    private String workDirectory;
    private String sid;
    private String name;
    private String status;
    private TaskLifecycle lifecycle;

    public TaskLifecycleEvent() {

    }

    public TaskLifecycleEvent(ServiceSetting setting, TaskLifecycle lifecycle) {
        this(setting.getWorkDirectory(), setting.getSid(), setting.getName(), lifecycle);
    }

    public TaskLifecycleEvent(String workDirectory, String sid, String name, TaskLifecycle lifecycle) {
        this.workDirectory = workDirectory;
        this.sid = sid;
        this.name = name;
        this.lifecycle = lifecycle;
        switch (lifecycle) {
            case PRE_STOP:
                this.status = CommonConst.STOPPING;
                break;
            case PRE_START:
                this.status = CommonConst.STARTING;
                break;
            case STOP_FAILED:
            case AFTER_STARTED:
                this.status = CommonConst.RUNNING;
                break;
            default:
                this.status = CommonConst.STOPPED;
                break;
        }
    }

    public String getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TaskLifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(TaskLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public String toString() {
        return "TaskLifecycleEvent{" +
                "workspace='" + workDirectory + '\'' +
                ", sid='" + sid + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", lifecycle=" + lifecycle +
                '}';
    }
}
