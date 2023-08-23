package io.github.majianzheng.jarboot.core.cmd.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ClassLoaderVO {
    private String name;
    private String hash;
    private String parent;
    private Integer loadedCount;
    private Integer numberOfInstances;
    private List<ClassLoaderVO> children;

    public ClassLoaderVO() {
        //do nothing
    }

    public void addChild(ClassLoaderVO child){
        if (this.children == null){
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Integer getLoadedCount() {
        return loadedCount;
    }

    public void setLoadedCount(Integer loadedCount) {
        this.loadedCount = loadedCount;
    }

    public Integer getNumberOfInstances() {
        return numberOfInstances;
    }

    public void setNumberOfInstances(Integer numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    public List<ClassLoaderVO> getChildren() {
        return children;
    }

    public void setChildren(List<ClassLoaderVO> children) {
        this.children = children;
    }
}
