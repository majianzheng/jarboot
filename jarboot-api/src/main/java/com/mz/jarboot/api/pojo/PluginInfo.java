package com.mz.jarboot.api.pojo;

/**
 * @author majianzheng
 */
public class PluginInfo {
    private String name;
    private String fileName;
    private String type;
    private Long lastModified;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "PluginInfoDTO{" +
                "name='" + name + '\'' +
                ", fileName='" + fileName + '\'' +
                ", type='" + type + '\'' +
                ", lastModified=" + lastModified +
                '}';
    }
}
