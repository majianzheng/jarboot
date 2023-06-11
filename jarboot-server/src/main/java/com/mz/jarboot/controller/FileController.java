package com.mz.jarboot.controller;

import com.mz.jarboot.api.pojo.FileNode;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.pojo.ResponseSimple;
import com.mz.jarboot.common.utils.HttpResponseUtils;
import com.mz.jarboot.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Autowired
    private FileService fileService;

    /**
     * 上传服务文件
     * @param file 文件
     * @param path 文件路径
     * @return 执行结果
     */
    @PostMapping("file")
    @ResponseBody
    public ResponseSimple upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("path") String path) throws IOException {
        try (InputStream is = file.getInputStream()) {
            fileService.uploadFile(path + File.separator + file.getOriginalFilename(), is);
        }
        return new ResponseSimple();
    }

    /**
     * 下载文件
     * @param path 文件
     * @param response
     * @throws IOException
     */
    @PostMapping("file/download")
    public void download(@RequestParam("path") String path, HttpServletResponse response) throws IOException {
        response.setHeader("content-type", "file");
        response.setContentType("application/octet-stream");
        try (OutputStream os = response.getOutputStream()) {
            fileService.download(path, os);
        }
    }

    /**
     * 获取文件列表
     * @param baseDir 文件目录
     * @param withRoot 是否包含baseDir
     * @return 文件列表
     */
    @PostMapping("list")
    public ResponseVo<List<FileNode>> getFiles(String baseDir, Boolean withRoot) {
        return HttpResponseUtils.success(fileService.getFiles(baseDir, withRoot));
    }

    /**
     * 获取文件内容
     * @param file 文件相对于工作目录的路径
     * @return 文件内容
     */
    @PostMapping("file/text")
    public ResponseVo<String> getContent(@RequestParam("path") String file) {
        return HttpResponseUtils.success(fileService.getContent(file));
    }

    /**
     * 删除文件
     * @param path 文件相对于工作目录的路径
     * @return
     */
    @PostMapping("file/delete")
    public ResponseVo<String> deleteFile(@RequestParam("path") String path) {
        fileService.deleteFile(path);
        return HttpResponseUtils.success();
    }

    /**
     * 写文件
     * @param path 文件相对于工作目录的路径
     * @param content 文件内容
     * @return
     */
    @PostMapping("text")
    public ResponseVo<String> writeFile(@RequestParam("path") String path, @RequestParam("content") String content) {
        return HttpResponseUtils.success(fileService.writeFile(path, content));
    }

    /**
     * 创建文本文件
     * @param path 文件相对于工作目录的路径
     * @param content 文件内容
     * @return
     */
    @PostMapping("text/create")
    public ResponseVo<String> newFile(@RequestParam("path") String path, @RequestParam("content") String content) {
        return HttpResponseUtils.success(fileService.newFile(path, content));
    }

    /**
     * 创建文件夹
     * @param file 文件相对于工作目录的路径
     * @return
     */
    @PostMapping("directory")
    public ResponseVo<String> addDirectory(@RequestParam("path") String file) {
        return HttpResponseUtils.success(fileService.addDirectory(file));
    }
}
