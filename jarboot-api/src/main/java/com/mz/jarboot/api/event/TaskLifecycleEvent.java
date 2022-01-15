package com.mz.jarboot.api.event;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.TaskLifecycle;
import com.mz.jarboot.api.pojo.ServerSetting;

/**
 * @author majianzheng
 */
public class TaskLifecycleEvent implements JarbootEvent {
    private String workspace;
    private String sid;
    private String name;
    private String status;
    private TaskLifecycle lifecycle;

    public TaskLifecycleEvent() {

    }

    public TaskLifecycleEvent(ServerSetting setting, TaskLifecycle lifecycle) {
        this(setting.getWorkspace(), setting.getSid(), setting.getName(), lifecycle);
    }

    public TaskLifecycleEvent(String workspace, String sid, String name, TaskLifecycle lifecycle) {
        this.workspace = workspace;
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

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
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
                "workspace='" + workspace + '\'' +
                ", sid='" + sid + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", lifecycle=" + lifecycle +
                '}';
    }
}
