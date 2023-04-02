package com.mz.jarboot.api.pojo;

/**
 * @author majianzheng
 */
public class ServiceInstance extends AbstractInstance {
    private String status;
    private String group;
    private String path;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "status='" + status + '\'' +
                ", name='" + getName() + '\'' +
                ", group='" + group + '\'' +
                ", sid='" + getSid() + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
