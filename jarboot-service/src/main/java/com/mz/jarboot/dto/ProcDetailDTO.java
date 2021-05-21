package com.mz.jarboot.dto;

public class ProcDetailDTO {
    private String pid;
    private String port;
    private String cmd;
    private String status;
    private String name;
    private String path;
    private String type;
    private Boolean daemon;
    private Boolean pathMonitor;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getDaemon() {
        return daemon;
    }

    public void setDaemon(Boolean daemon) {
        this.daemon = daemon;
    }

    public Boolean getPathMonitor() {
        return pathMonitor;
    }

    public void setPathMonitor(Boolean pathMonitor) {
        this.pathMonitor = pathMonitor;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
