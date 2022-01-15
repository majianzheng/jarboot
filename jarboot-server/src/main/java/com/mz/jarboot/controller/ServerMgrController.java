package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.*;
import com.mz.jarboot.api.service.ServerMgrService;
import com.mz.jarboot.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 服务管理
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.SERVER_MGR_CONTEXT)
@Controller
@Permission
public class ServerMgrController {
    @Autowired
    private ServerMgrService serverMgrService;

    /**
     * 获取服务列表
     * @return 服务列表
     */
    @GetMapping(value="/getServerList")
    @ResponseBody
    public ResponseForList<ServerRunning> getServerList() {
        List<ServerRunning> results = serverMgrService.getServiceList();
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
        serverMgrService.startService(servers);
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
        serverMgrService.stopService(servers);
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
        serverMgrService.restartService(servers);
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
     * 获取未被服务管理的JVM进程信息
     * @return 进程列表
     */
    @GetMapping(value="/getJvmProcesses")
    @ResponseBody
    public ResponseForList<JvmProcess> getJvmProcesses() {
        List<JvmProcess> results = serverMgrService.getJvmProcesses();
        return new ResponseForList<>(results, results.size());
    }

    /**
     * attach进程
     * @return 执行结果
     */
    @GetMapping(value="/attach")
    @ResponseBody
    public ResponseSimple attach(String pid) {
        serverMgrService.attach(pid);
        return new ResponseSimple();
    }

    /**
     * 删除服务
     * @return 执行结果
     */
    @DeleteMapping(value="/service")
    @ResponseBody
    @Permission
    public ResponseSimple deleteServer(String serviceName) {
        serverMgrService.deleteService(serviceName);
        return new ResponseSimple();
    }

    /**
     * 获取服务信息
     * @return 服务信息
     */
    @GetMapping(value="/service")
    @ResponseBody
    public ResponseForObject<ServerRunning> getServer(String serviceName) {
        ServerRunning result = serverMgrService.getService(serviceName);
        return new ResponseForObject<>(result);
    }
}
