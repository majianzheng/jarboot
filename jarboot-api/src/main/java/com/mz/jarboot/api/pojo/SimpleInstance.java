package com.mz.jarboot.api.pojo;

import java.util.List;

/**
 * @author mazheng
 */
public class SimpleInstance {
    /** 集群模式下要指定，非集群忽略 */
    private String host;
    private String sid;
    private String name;

    private List<SimpleInstance> children;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SimpleInstance> getChildren() {
        return children;
    }

    public void setChildren(List<SimpleInstance> children) {
        this.children = children;
    }
}
