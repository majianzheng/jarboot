package com.mz.jarboot.event;

import com.mz.jarboot.api.event.JarbootEvent;

/**
 * @author majianzheng
 */
public class OfflineEvent implements JarbootEvent {
    private String server;
    private String sid;

    public OfflineEvent(String server, String sid) {
        this.server = server;
        this.sid = sid;
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
