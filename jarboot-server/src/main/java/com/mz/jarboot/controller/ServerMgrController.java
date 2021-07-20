package com.mz.jarboot.controller;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.*;
import com.mz.jarboot.dto.*;
import com.mz.jarboot.service.ServerMgrService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Api(tags="服务管理")
@RequestMapping(value = "/api/jarboot/services")
@Controller
@Permission
public class ServerMgrController {
    @Autowired
    private ServerMgrService serverMgrService;

    @ApiOperation(value = "获取服务列表", httpMethod = "GET")
    @GetMapping(value="/getServerList")
    @ResponseBody
    public ResponseForList<ServerRunningDTO> getWebServerList() {
        List<ServerRunningDTO> results = serverMgrService.getServerList();
        return new ResponseForList<>(results, results.size());
    }

    @ApiOperation(value = "启动服务", httpMethod = "POST")
    @PostMapping(value="/startServer")
    @ResponseBody
    @Permission
    public ResponseSimple startServer(@RequestBody List<String> servers) {
        serverMgrService.startServer(servers);
        return new ResponseSimple();
    }
    @ApiOperation(value = "停止服务", httpMethod = "POST")
    @PostMapping(value="/stopServer")
    @ResponseBody
    @Permission
    public ResponseSimple stopServer(@RequestBody List<String> servers) {
        serverMgrService.stopServer(servers);
        return new ResponseSimple();
    }
    @ApiOperation(value = "重启服务", httpMethod = "POST")
    @PostMapping(value="/restartServer")
    @ResponseBody
    @Permission
    public ResponseSimple restartServer(@RequestBody List<String> servers) {
        serverMgrService.restartServer(servers);
        return new ResponseSimple();
    }

    @ApiOperation(value = "一键重启", httpMethod = "GET")
    @GetMapping(value="/oneClickRestart")
    @ResponseBody
    @Permission
    public ResponseSimple oneClickRestart() {
        serverMgrService.oneClickRestart();
        return new ResponseSimple();
    }

    @ApiOperation(value = "一键启动", httpMethod = "GET")
    @GetMapping(value="/oneClickStart")
    @ResponseBody
    @Permission
    public ResponseSimple oneClickStart() {
        serverMgrService.oneClickStart();
        return new ResponseSimple();
    }

    @ApiOperation(value = "一键停止", httpMethod = "GET")
    @GetMapping(value="/oneClickStop")
    @ResponseBody
    @Permission
    public ResponseSimple oneClickStop() {
        serverMgrService.oneClickStop();
        return new ResponseSimple();
    }

    @ApiOperation(value = "base64编码", httpMethod = "GET")
    @GetMapping(value="/base64Encoder")
    @ResponseBody
    public ResponseForObject<String> base64Encoder(@ApiParam(value = "字符串数据", required = true) String data) {
        if (StringUtils.isEmpty(data)) {
            return new ResponseForObject<>(ResultCodeConst.EMPTY_PARAM, "参数为空");
        }
        data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        return new ResponseForObject<>(data);
    }

    /**
     * 从服务器下载文件
     * @param file base64编码的文件全路径名
     * @param response
     */
    @ApiOperation(value = "下载文件", httpMethod = "GET")
    @GetMapping(value="/downloadFile/{file}")
    @Permission
    public void downloadFile(@PathVariable("file") @ApiParam(value = "文件全路径(base64编码)", required = true) String file,
                             HttpServletResponse response) {
        //待下载文件名
        String fileName = new String(Base64.getDecoder().decode(file));
        File target = FileUtils.getFile(fileName);
        if (!target.exists() || !target.isFile()) {
            return;
        }
        response.setHeader("content-type", "file");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + target.getName());
        byte[] buff = new byte[2048];
        //创建缓冲输入流
        try (FileInputStream fis = new FileInputStream(target);
             OutputStream outputStream = response.getOutputStream();
             BufferedInputStream bis = new BufferedInputStream(fis);){
            int len = -1;
            while (-1 != (len = bis.read(buff))) {
                outputStream.write(buff, 0, len);
            }
            outputStream.flush();
        } catch ( Exception e ) {
            //
        }
    }
}
