package io.github.majianzheng.jarboot.api.event;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.TaskLifecycle;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;

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
            case SCHEDULING:
                this.status = CommonConst.SCHEDULING;
                break;
            default:
                this.status = CommonConst.STOPPED;
                break;
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
