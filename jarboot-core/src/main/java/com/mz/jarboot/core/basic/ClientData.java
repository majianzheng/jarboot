package com.mz.jarboot.core.basic;

/**
 * @author majianzheng
 */
public class ClientData {
    private String server;
    private String sid;
    private String host;
    private boolean hostRemote;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isHostRemote() {
        return hostRemote;
    }

    public void setHostRemote(boolean hostRemote) {
        this.hostRemote = hostRemote;
    }

    @Override
    public String toString() {
        return "ClientData{" +
                "server='" + server + '\'' +
                ", sid='" + sid + '\'' +
                ", host='" + host + '\'' +
                ", hostRemote=" + hostRemote +
                '}';
    }
}
