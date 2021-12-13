package com.mz.jarboot.core.basic;

/**
 * @author majianzheng
 */
public class ClientData {
    private String server;
    private String sid;
    private String host;
    private boolean diagnose;

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

    public boolean isDiagnose() {
        return diagnose;
    }

    public void setDiagnose(boolean diagnose) {
        this.diagnose = diagnose;
    }

    @Override
    public String toString() {
        return "ClientData{" +
                "server='" + server + '\'' +
                ", sid='" + sid + '\'' +
                ", host='" + host + '\'' +
                ", diagnose=" + diagnose +
                '}';
    }
}
