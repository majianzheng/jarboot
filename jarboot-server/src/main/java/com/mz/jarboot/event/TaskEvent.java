package com.mz.jarboot.event;

import java.util.List;

/**
 * @author majianzheng
 */
public class TaskEvent {
    private TaskEventEnum eventType;
    private List<String> paths;
    private String server;
    private String sid;

    public TaskEvent(TaskEventEnum type) {
        this.eventType = type;
    }

    public TaskEvent(TaskEventEnum type, String server, String sid) {
        this.eventType = type;
        this.server = server;
        this.sid = sid;
    }

    public TaskEventEnum getEventType() {
        return eventType;
    }

    public void setEventType(TaskEventEnum eventType) {
        this.eventType = eventType;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }
}
