package com.mz.jarboot.controller;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.service.UploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传
 */
@RequestMapping(value = "/api/jarboot/upload")
@Controller
@Permission
public class UploadFileController {
    @Autowired
    private UploadFileService uploadFileService;

    /**
     * 上传服务文件
     * @param file 文件
     * @param server 服务名
     * @return 执行结果
     */
    @PostMapping(value="/upload")
    @ResponseBody
    public ResponseSimple upload(@RequestParam("file") MultipartFile file, @RequestParam("server") String server) {
        uploadFileService.uploadJarFiles(file, server);
        return new ResponseSimple();
    }

    /**
     * 开始上传服务的文件
     * @param server 服务名
     * @return 执行结果
     */
    @GetMapping(value="/beginUploadServerFile")
    @ResponseBody
    @Permission("Upload file")
    public ResponseSimple beginUploadServerFile(String server) {
        uploadFileService.beginUploadServerFile(server);
        return new ResponseSimple();
    }

    /**
     * 上传文件心跳
     * @param server 服务名
     * @return 执行结果
     */
    @GetMapping(value="/uploadServerHeartbeat")
    @ResponseBody
    public ResponseSimple uploadServerHeartbeat(String server) {
        uploadFileService.uploadServerHeartbeat(server);
        return new ResponseSimple();
    }

    /**
     * 删除上传缓冲区的文件
     * @param server 服务名
     * @param file 文件
     * @return 执行结果
     */
    @DeleteMapping(value="/deleteFileInUploadCache")
    @ResponseBody
    public ResponseSimple deleteFileInUploadCache(@RequestParam("server") String server, @RequestParam("file") String file) {
        uploadFileService.deleteUploadFileInCache(server, file);
        return new ResponseSimple();
    }

    /**
     * 提交上传缓冲区的文件
     * @param server 服务名
     * @return 执行结果
     */
    @PostMapping(value="/submitUploadFileInCache")
    @ResponseBody
    public ResponseSimple submitUploadFileInCache(@RequestParam("server") String server) {
        uploadFileService.submitUploadFileInCache(server);
        return new ResponseSimple();
    }

    /**
     * 清空上传缓冲区的文件
     * @param server 服务名
     * @return 执行结果
     */
    @DeleteMapping(value="/clearUploadFileInCache")
    @ResponseBody
    public ResponseSimple clearUploadFileInCache(@RequestParam("server") String server) {
        uploadFileService.clearUploadFileInCache(server);
        return new ResponseSimple();
    }
}
