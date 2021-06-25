package com.mz.jarboot.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadFileService {
    /**
     * 开始上传服务文件
     * @param server 服务名
     */
    void beginUploadServerFile(String server);

    /**
     * 上传文件心跳
     * @param server 服务名
     */
    void uploadServerHeartbeat(String server);

    /**
     * 提交已经上传的文件
     * @param server 服务名
     */
    void submitUploadFileInCache(String server);

    /**
     * 删除已上传到缓冲区到文件
     * @param server 服务名
     * @param file 文件名
     */
    void deleteUploadFileInCache(String server, String file);

    /**
     * 提交已经上传的文件
     * @param server 服务名
     */
    void clearUploadFileInCache(String server);

    /**
     * 更新或新增服务的文件
     * @param file 上传的jar或zip文件
     * @param server 服务名
     */
    void uploadJarFiles(MultipartFile file, String server);
}
