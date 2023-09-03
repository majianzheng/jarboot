package io.github.majianzheng.jarboot.api.pojo;

import java.util.List;

/**
 * 文件节点
 * @author mazheng
 */
public class FileNode {
    /** 文件名 */
    private String name;

    /** 是否为目录 */
    private Boolean directory;

    /** 父节点 */
    private String parent;

    private String key;

    private Long modifyTime;

    private Long size;

    private Boolean leaf;

    /** 子文件 */
    private List<FileNode> children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDirectory() {
        return directory;
    }

    public void setDirectory(Boolean directory) {
        this.directory = directory;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Long modifyTime) {
        this.modifyTime = modifyTime;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getLeaf() {
        return leaf;
    }

    public void setLeaf(Boolean leaf) {
        this.leaf = leaf;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileNode> children) {
        this.children = children;
    }
}
