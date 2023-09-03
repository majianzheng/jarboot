package io.github.majianzheng.jarboot.service;

import io.github.majianzheng.jarboot.api.pojo.FileNode;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件管理
 * @author mazheng
 */
public interface FileService {
    /**
     * 获取文件列表
     * @param baseDir 路径
     * @param withRoot 是否包含根路径
     * @return 文件节点
     */
    List<FileNode> getWorkspaceFiles(String baseDir, boolean withRoot);

    /**
     * 获取文件内容
     * @param file 文件相对于工作目录的路径
     * @return 文件内容
     */
    String getContent(String file);

    /**
     * 删除文件
     * @param file 文件相对于工作目录的路径
     */
    void deleteFile(String file);

    /**
     * 上传文件
     * @param file 文件相对于工作目录的路径
     * @param is 文件流
     */
    void uploadFile(String file, InputStream is);

    /**
     * 下载文件
     * @param file 文件相对于工作目录的路径
     * @param os 文件流
     */
    void download(String file, OutputStream os);

    /**
     * 修改文件
     * @param file 文件相对于工作目录的路径
     * @param content 文件内容
     * @return key
     */
    String writeFile(String file, String content);

    /**
     * 新建文件
     * @param file 文件相对于工作目录的路径
     * @param content 文件内容
     * @return key
     */
    String newFile(String file, String content);

    /**
     * 新建文件夹
     * @param file 文件相对于工作目录的路径
     * @return key
     */
    String addDirectory(String file);
}
