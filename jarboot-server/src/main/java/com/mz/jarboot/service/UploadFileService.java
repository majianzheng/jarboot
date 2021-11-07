package com.mz.jarboot.service;

import com.mz.jarboot.api.pojo.ServerSetting;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author majianzheng
 */
public interface UploadFileService {
    /**
     * 开始上传服务文件
     * @param server 服务名
     * @return {boolean} 是否存在服务目录
     */
    boolean startUpload(String server);

    /**
     * 上传文件心跳
     * @param server 服务名
     */
    void uploadHeartbeat(String server);

    /**
     * 提交已经上传的文件
     * @param server 服务名
     */
    void submitUploadFile(ServerSetting server);

    /**
     * 删除已上传到缓冲区到文件
     * @param server 服务名
     * @param file 文件名
     */
    void deleteUploadFile(String server, String file);

    /**
     * 提交已经上传的文件
     * @param server 服务名
     */
    void clearUploadCache(String server);

    /**
     * 更新或新增服务的文件
     * @param file 上传的jar或zip文件
     * @param server 服务名
     */
    void uploadJarFiles(MultipartFile file, String server);
}
