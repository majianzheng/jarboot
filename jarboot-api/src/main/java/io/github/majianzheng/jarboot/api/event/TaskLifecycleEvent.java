package io.github.majianzheng.jarboot.api.event;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.TaskLifecycle;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;

import java.util.Objects;

/**
 * @author majianzheng
 */
public class TaskLifecycleEvent implements JarbootEvent {
    private ServiceSetting setting;
    private String status;
    private TaskLifecycle lifecycle;

    public TaskLifecycleEvent() {

    }

    public TaskLifecycleEvent(ServiceSetting setting, TaskLifecycle lifecycle) {
        this.setting = setting;
        this.init(lifecycle);
    }

    private void init(TaskLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        if (Objects.equals(TaskLifecycle.PRE_STOP, lifecycle)) {
            this.status = CommonConst.STOPPING;
        } else if (Objects.equals(TaskLifecycle.PRE_START, lifecycle)) {
            this.status = CommonConst.STARTING;
        } else if (Objects.equals(TaskLifecycle.STOP_FAILED, lifecycle) || Objects.equals(TaskLifecycle.AFTER_STARTED, lifecycle)) {
            this.status = CommonConst.RUNNING;
        } else if (Objects.equals(TaskLifecycle.SCHEDULING, lifecycle)) {
            this.status = CommonConst.SCHEDULING;
        } else {
            this.status = CommonConst.STOPPED;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLifecycle(TaskLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public TaskLifecycle getLifecycle() {
        return lifecycle;
    }

    public void setSetting(ServiceSetting setting) {
        this.setting = setting;
    }

    public ServiceSetting getSetting() {
        return setting;
    }


    @Override
    public String toString() {
        return "TaskLifecycleEvent{" +
                "setting=" + setting.toString() +
                ", status='" + status + '\'' +
                ", lifecycle=" + lifecycle +
                '}';
    }
}
