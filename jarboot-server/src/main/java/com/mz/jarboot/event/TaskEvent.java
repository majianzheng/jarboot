package com.mz.jarboot.event;

import java.util.List;

/**
 * @author jianzhengma
 */
public class TaskEvent {
    private TaskEventEnum eventType;
    private List<String> services;

    public TaskEventEnum getEventType() {
        return eventType;
    }

    public void setEventType(TaskEventEnum eventType) {
        this.eventType = eventType;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
