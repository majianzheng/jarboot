package io.github.majianzheng.jarboot.common.pojo;

import lombok.Data;

@Data
public class UploadFileParam {
    private String clusterHost;
    private String filename;
    private String dstPath;
    private String relativePath;
    private String baseDir;
    private Long totalSize;
    private long sendCountOnce;
    private String uploadMode;
}
