package com.mz.jarboot.api.pojo;

/**
 * @author majianzheng
 */
public class ServiceInstance extends SimpleInstance {
    private String status;
    private String group;

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

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "status='" + status + '\'' +
                ", name='" + getName() + '\'' +
                ", group='" + group + '\'' +
                ", sid='" + getSid() + '\'' +
                '}';
    }
}
