package com.mz.jarboot.controller;

import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.service.OnlineDebugService;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.*;
import com.mz.jarboot.api.service.ServerMgrService;
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

/**
 * 服务管理
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/services")
@Controller
@Permission
public class ServerMgrController {
    @Autowired
    private ServerMgrService serverMgrService;
    @Autowired
    private OnlineDebugService onlineDebugService;

    /**
     * 获取服务列表
     * @return 服务列表
     */
    @GetMapping(value="/getServerList")
    @ResponseBody
    public ResponseForList<ServerRunning> getServerList() {
        List<ServerRunning> results = serverMgrService.getServerList();
        return new ResponseForList<>(results, results.size());
    }

    /**
     * 启动服务
     * @param servers 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/startServer")
    @ResponseBody
    @Permission
    public ResponseSimple startServer(@RequestBody List<String> servers) {
        serverMgrService.startServer(servers);
        return new ResponseSimple();
    }

    /**
     * 停止服务
     * @param servers 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/stopServer")
    @ResponseBody
    @Permission
    public ResponseSimple stopServer(@RequestBody List<String> servers) {
        serverMgrService.stopServer(servers);
        return new ResponseSimple();
    }

    /**
     * 重启服务
     * @param servers 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/restartServer")
    @ResponseBody
    @Permission
    public ResponseSimple restartServer(@RequestBody List<String> servers) {
        serverMgrService.restartServer(servers);
        return new ResponseSimple();
    }

    /**
     * 一键重启
     * @return 执行结果
     */
    @GetMapping(value="/oneClickRestart")
    @ResponseBody
    @Permission
    public ResponseSimple oneClickRestart() {
        serverMgrService.oneClickRestart();
        return new ResponseSimple();
    }

    /**
     * 一键启动
     * @return 执行结果
     */
    @GetMapping(value="/oneClickStart")
    @ResponseBody
    @Permission
    public ResponseSimple oneClickStart() {
        serverMgrService.oneClickStart();
        return new ResponseSimple();
    }

    /**
     * 一键停止
     * @return 执行结果
     */
    @GetMapping(value="/oneClickStop")
    @ResponseBody
    @Permission
    public ResponseSimple oneClickStop() {
        serverMgrService.oneClickStop();
        return new ResponseSimple();
    }

    /**
     * base64编码
     * @param data 数据
     * @return 编码后的数据
     */
    @GetMapping(value="/base64Encoder")
    @ResponseBody
    public ResponseForObject<String> base64Encoder(String data) {
        if (StringUtils.isEmpty(data)) {
            return new ResponseForObject<>(ResultCodeConst.EMPTY_PARAM, "参数为空");
        }
        data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        return new ResponseForObject<>(data);
    }

    /**
     * 从服务器下载文件
     * @param file base64编码的文件全路径名
     * @param response Servlet response
     */
    @GetMapping(value="/downloadFile/{file}")
    public void downloadFile(@PathVariable("file") String file,
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

    /**
     * 获取未被服务管理的JVM进程信息
     * @return 进程列表
     */
    @GetMapping(value="/getJvmProcesses")
    @ResponseBody
    public ResponseForList<JvmProcess> getJvmProcesses() {
        List<JvmProcess> results = onlineDebugService.getJvmProcesses();
        return new ResponseForList<>(results, results.size());
    }

    /**
     * attach进程
     * @return 执行结果
     */
    @PostMapping(value="/attach")
    @ResponseBody
    @Permission
    public ResponseSimple attach(int pid, String name) {
        onlineDebugService.attach(pid, name);
        return new ResponseSimple();
    }
}
