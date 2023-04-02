package com.mz.jarboot.api.pojo;

import java.util.List;

/**
 * @author mazheng
 */
public class AbstractInstance {
    private String sid;
    private String name;

    private List<AbstractInstance> children;

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

    public List<AbstractInstance> getChildren() {
        return children;
    }

    public void setChildren(List<AbstractInstance> children) {
        this.children = children;
    }
}
