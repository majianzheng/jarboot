package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.pojo.FileNode;
import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.annotation.EnableAuditLog;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.FileService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件管理
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/file-manager")
@RestController
public class FileController {
    @Resource
    private FileService fileService;

    /**
     * 上传服务文件
     * @param file 文件
     * @param clusterHost 集群host
     * @param path 文件路径
     * @return 执行结果
     */
    @PostMapping("file")
    @EnableAuditLog("上传文件")
    public ResponseSimple upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String clusterHost,
            @RequestParam("path") String path) throws IOException {
        try (InputStream is = file.getInputStream()) {
            if (CommonUtils.needProxy(clusterHost)) {
                ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
                client.upload(file.getOriginalFilename(), path, is);
            } else {
                fileService.uploadFile(path + File.separator + file.getOriginalFilename(), is);
            }
        }
        return HttpResponseUtils.success();
    }

    /**
     * 下载文件
     * @param path 文件
     * @param clusterHost 集群host
     * @param response 响应
     * @throws IOException IO异常
     */
    @PostMapping("file/download")
    @EnableAuditLog("下载文件")
    public void download(
            @RequestParam("path") String path,
            @RequestParam(required = false) String clusterHost,
            HttpServletResponse response) throws IOException {
        response.setHeader("content-type", "file");
        response.setContentType("application/octet-stream");
        try (OutputStream os = response.getOutputStream()) {
            if (CommonUtils.needProxy(clusterHost)) {
                ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
                client.download(path, os);
            } else {
                fileService.download(path, os);
            }
        }
    }

    /**
     * 获取文件列表
     * @param baseDir 文件目录
     * @param clusterHost 集群host
     * @param withRoot 是否包含baseDir
     * @return 文件列表
     */
    @PostMapping("list")
    public ResponseVo<List<FileNode>> getFiles(
            String baseDir,
            @RequestParam(required = false) String clusterHost,
            boolean withRoot) {
        if (CommonUtils.needProxy(clusterHost)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
            return HttpResponseUtils.success(client.fileList(baseDir, withRoot));
        } else {
            return HttpResponseUtils.success(fileService.getWorkspaceFiles(baseDir, withRoot));
        }
    }

    /**
     * 获取文件内容
     * @param file 文件相对于工作目录的路径
     * @param clusterHost 集群host
     * @return 文件内容
     */
    @PostMapping("file/text")
    public ResponseVo<String> getContent(
            @RequestParam("path") String file,
            @RequestParam(required = false) String clusterHost) {
        if (CommonUtils.needProxy(clusterHost)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
            return HttpResponseUtils.success(client.getFileContent(file));
        } else {
            return HttpResponseUtils.success(fileService.getContent(file));
        }
    }

    /**
     * 删除文件
     * @param clusterHost 集群host
     * @param path 文件相对于工作目录的路径
     * @return 执行结果
     */
    @DeleteMapping("file/delete")
    @EnableAuditLog("删除文件")
    public ResponseVo<String> deleteFile(
            @RequestParam(required = false) String clusterHost,
            @RequestParam("path") String path) {
        if (CommonUtils.needProxy(clusterHost)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
            client.deleteFile(path);
        } else {
            fileService.deleteFile(path);
        }
        return HttpResponseUtils.success();
    }

    /**
     * 写文件
     * @param clusterHost 集群host
     * @param path 文件相对于工作目录的路径
     * @param content 文件内容
     * @return 执行结果
     */
    @PostMapping("text")
    @EnableAuditLog("写文件")
    public ResponseVo<String> writeFile(
            @RequestParam(required = false) String clusterHost,
            @RequestParam("path") String path,
            @RequestParam("content") String content) {
        if (CommonUtils.needProxy(clusterHost)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
            return HttpResponseUtils.success(client.writeFileContent(path, content));
        } else {
            return HttpResponseUtils.success(fileService.writeFile(path, content));
        }
    }

    /**
     * 创建文本文件
     * @param clusterHost 集群host
     * @param path 文件相对于工作目录的路径
     * @param content 文件内容
     * @return 执行结果
     */
    @PostMapping("text/create")
    @EnableAuditLog("创建文本文件")
    public ResponseVo<String> newFile(
            @RequestParam(required = false) String clusterHost,
            @RequestParam("path") String path,
            @RequestParam("content") String content) {
        if (CommonUtils.needProxy(clusterHost)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
            return HttpResponseUtils.success(client.newFile(path, content));
        } else {
            return HttpResponseUtils.success(fileService.newFile(path, content));
        }
    }

    /**
     * 创建文件夹
     * @param clusterHost 集群host
     * @param file 文件相对于工作目录的路径
     * @return 执行结果
     */
    @PostMapping("directory")
    @EnableAuditLog("创建文件夹")
    public ResponseVo<String> addDirectory(
            @RequestParam(required = false) String clusterHost,
            @RequestParam("path") String file) {
        if (CommonUtils.needProxy(clusterHost)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
            return HttpResponseUtils.success(client.newDirectory(file));
        } else {
            return HttpResponseUtils.success(fileService.addDirectory(file));
        }
    }
}
