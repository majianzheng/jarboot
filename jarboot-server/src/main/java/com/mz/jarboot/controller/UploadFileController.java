package com.mz.jarboot.controller;

import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.service.UploadFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/upload")
@RestController
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
    @PostMapping
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
    @GetMapping(value="/start")
    @ResponseBody
    @Permission("Upload file")
    public ResponseForObject<Boolean> startUpload(String server) {
        boolean exist = uploadFileService.startUpload(server);
        return new ResponseForObject<>(exist);
    }

    /**
     * 上传文件心跳
     * @param server 服务名
     * @return 执行结果
     */
    @GetMapping(value="/heartbeat")
    @ResponseBody
    public ResponseSimple uploadHeartbeat(String server) {
        uploadFileService.uploadHeartbeat(server);
        return new ResponseSimple();
    }

    /**
     * 删除上传缓冲区的文件
     * @param server 服务名
     * @param file 文件
     * @return 执行结果
     */
    @DeleteMapping(value="/file")
    @ResponseBody
    public ResponseSimple deleteUploadFile(@RequestParam("server") String server, @RequestParam("file") String file) {
        uploadFileService.deleteUploadFile(server, file);
        return new ResponseSimple();
    }

    /**
     * 提交上传缓冲区的文件
     * @param setting 服务配置
     * @return 执行结果
     */
    @PostMapping(value="/file")
    @ResponseBody
    public ResponseSimple submitUploadFile(@RequestBody ServerSetting setting) {
        uploadFileService.submitUploadFile(setting);
        return new ResponseSimple();
    }

    /**
     * 清空上传缓冲区的文件
     * @param server 服务名
     * @return 执行结果
     */
    @DeleteMapping
    @ResponseBody
    public ResponseSimple clearUploadCache(@RequestParam("server") String server) {
        uploadFileService.clearUploadCache(server);
        return new ResponseSimple();
    }
}
