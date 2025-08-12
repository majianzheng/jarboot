package io.github.majianzheng.jarboot.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * 文件上传进度表
 * @author mazheng
 */
@Table(name = FileUploadProgress.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"clusterHost", "absolutePath"})})
@Entity
public class FileUploadProgress extends AbstractBaseEntity {
    public static final String TABLE_NAME = "jarboot_file_upload_progress";
    private String clusterHost;
    private String filename;
    private String dstPath;
    private String absolutePath;
    private String relativePath;
    private Long totalSize;
    private Long uploadSize;
    @Transient
    private String errorMsg;
    @Transient
    private String event;

    public String getClusterHost() {
        return clusterHost;
    }

    public void setClusterHost(String clusterHost) {
        this.clusterHost = clusterHost;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDstPath() {
        return dstPath;
    }

    public void setDstPath(String dstPath) {
        this.dstPath = dstPath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Long getUploadSize() {
        return uploadSize;
    }

    public void setUploadSize(Long uploadSize) {
        this.uploadSize = uploadSize;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }
}
