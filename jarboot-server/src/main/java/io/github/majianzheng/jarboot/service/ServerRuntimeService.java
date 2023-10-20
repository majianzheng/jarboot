package io.github.majianzheng.jarboot.service;

import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mazheng
 */
public interface ServerRuntimeService {
    /**
     * 获取Jarboot运行时信息
     * @return 运行时信息
     */
    ServerRuntimeInfo getServerRuntimeInfo();

    /**
     * 获取UUID
     * @return uuid
     */
    String getUuid();

    /**
     * 导出服务
     * @param name 服务名
     * @param os 输出流
     */
    void exportService(String name, OutputStream os);

    /**
     * 导入服务
     * @param name 文件名
     * @param file 文件
     */
    void importService(String name, InputStream file);

    /**
     * 恢复服务
     * @param username 用户名
     * @param serviceZip 导出的压缩包
     */
    void recoverService(String username, File serviceZip);

    /**
     * 下载文件
     * @param encodedFilePath 路径
     * @param os 输出流
     */
    void downloadAnyFile(String encodedFilePath, OutputStream os);
}
