package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Node of TraceCommand
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public abstract class TraceNode {
    protected TraceNode parent;
    protected List<TraceNode> children;

    /**
     * node type: method,
     */
    private String type;

    /**
     * 备注
     */
    private String mark;

    private int marks = 0;

    protected TraceNode(String type) {
        this.type = type;
    }

    public void addChild(TraceNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        this.children.add(child);
        child.setParent(this);
    }

    public void setMark(String mark) {
        this.mark = mark;
        marks++;
    }

    public String getMark() {
        return mark;
    }

    public Integer marks() {
        return marks;
    }

    public void begin() {
    }

    public void end() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TraceNode parent() {
        return parent;
    }

    public void setParent(TraceNode parent) {
        this.parent = parent;
    }

    public List<TraceNode> getChildren() {
        return children;
    }
}
