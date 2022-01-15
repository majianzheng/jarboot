package com.mz.jarboot.api.pojo;

/**
 * @author majianzheng
 */
public class ServiceInstance {
    private String status;
    private String name;
    private String group;
    private String sid;
    private String path;

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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ServerRunning{" +
                "status='" + status + '\'' +
                ", name='" + name + '\'' +
                ", path=" + path +
                ", sid=" + sid +
                '}';
    }
}
