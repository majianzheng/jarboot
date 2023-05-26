package com.mz.jarboot.controller;

import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.pojo.ResponseSimple;
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
public class UploadServiceFileController {
    @Autowired
    private UploadFileService uploadFileService;

    /**
     * 上传服务文件
     * @param file 文件
     * @param serviceName 服务名
     * @return 执行结果
     */
    @PostMapping
    @ResponseBody
    public ResponseSimple upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("serviceName") String serviceName) {
        uploadFileService.uploadJarFiles(file, serviceName);
        return new ResponseSimple();
    }

    /**
     * 开始上传服务的文件
     * @param serviceName 服务名
     * @return 执行结果
     */
    @GetMapping(value="/start")
    @ResponseBody
    @Permission("Upload service file")
    public ResponseVo<Boolean> startUpload(String serviceName) {
        boolean exist = uploadFileService.startUpload(serviceName);
        return new ResponseVo<>(exist);
    }

    /**
     * 上传文件心跳
     * @param serviceName 服务名
     * @return 执行结果
     */
    @GetMapping(value="/heartbeat")
    @ResponseBody
    public ResponseSimple uploadHeartbeat(String serviceName) {
        uploadFileService.uploadHeartbeat(serviceName);
        return new ResponseSimple();
    }

    /**
     * 删除上传缓冲区的文件
     * @param serviceName 服务名
     * @param file 文件
     * @return 执行结果
     */
    @DeleteMapping(value="/file")
    @ResponseBody
    public ResponseSimple deleteUploadFile(String serviceName, String file) {
        uploadFileService.deleteUploadFile(serviceName, file);
        return new ResponseSimple();
    }

    /**
     * 提交上传缓冲区的文件
     * @param setting 服务配置
     * @return 执行结果
     */
    @PostMapping(value="/file")
    @ResponseBody
    public ResponseSimple submitUploadFile(@RequestBody ServiceSetting setting) {
        uploadFileService.submitUploadFile(setting);
        return new ResponseSimple();
    }

    /**
     * 清空上传缓冲区的文件
     * @param serviceName 服务名
     * @return 执行结果
     */
    @DeleteMapping
    @ResponseBody
    public ResponseSimple clearUploadCache(String serviceName) {
        uploadFileService.clearUploadCache(serviceName);
        return new ResponseSimple();
    }
}
