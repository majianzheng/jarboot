package com.mz.jarboot.controller;

import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.service.UploadFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags="文件上传")
@RequestMapping(value = "/api/jarboot-upload", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class UploadFileController {
    @Autowired
    private UploadFileService uploadFileService;

    @ApiOperation(value = "上传jar或zip文件", httpMethod = "POST")
    @PostMapping(value="/upload")
    @ResponseBody
    public ResponseSimple upload(@RequestParam("file") MultipartFile file, @RequestParam("server") String server) {
        uploadFileService.uploadJarFiles(file, server);
        return new ResponseSimple();
    }

    @ApiOperation(value = "开始上传服务的文件", httpMethod = "GET")
    @GetMapping(value="/beginUploadServerFile")
    @ResponseBody
    public ResponseSimple beginUploadServerFile(String server) {
        uploadFileService.beginUploadServerFile(server);
        return new ResponseSimple();
    }

    @ApiOperation(value = "上传文件心跳", httpMethod = "GET")
    @GetMapping(value="/uploadServerHeartbeat")
    @ResponseBody
    public ResponseSimple uploadServerHeartbeat(String server) {
        uploadFileService.uploadServerHeartbeat(server);
        return new ResponseSimple();
    }

    @ApiOperation(value = "删除上传缓冲区的文件", httpMethod = "DELETE")
    @DeleteMapping(value="/deleteFileInUploadCache")
    @ResponseBody
    public ResponseSimple deleteFileInUploadCache(@RequestParam("server") String server, @RequestParam("file") String file) {
        uploadFileService.deleteUploadFileInCache(server, file);
        return new ResponseSimple();
    }

    @ApiOperation(value = "提交上传缓冲区的文件", httpMethod = "POST")
    @PostMapping(value="/submitUploadFileInCache")
    @ResponseBody
    public ResponseSimple submitUploadFileInCache(@RequestParam("server") String server) {
        uploadFileService.submitUploadFileInCache(server);
        return new ResponseSimple();
    }

    @ApiOperation(value = "清空上传缓冲区的文件", httpMethod = "DELETE")
    @DeleteMapping(value="/clearUploadFileInCache")
    @ResponseBody
    public ResponseSimple clearUploadFileInCache(@RequestParam("server") String server) {
        uploadFileService.clearUploadFileInCache(server);
        return new ResponseSimple();
    }
}
