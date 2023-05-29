package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServiceGroup;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.api.service.ServiceManager;
import com.mz.jarboot.common.pojo.PagedList;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.pojo.ResponseSimple;
import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.common.utils.HttpResponseUtils;
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
@RequestMapping(value = CommonConst.SERVICE_MGR_CONTEXT)
@Controller
@Permission
public class ServiceMgrController {
    @Autowired
    private ServiceManager serviceManager;

    /**
     * 获取服务列表
     * @return 服务列表
     */
    @GetMapping
    @ResponseBody
    public ResponseVo<List<ServiceInstance>> getServiceList() {
        List<ServiceInstance> results = serviceManager.getServiceList();
        return HttpResponseUtils.success(results);
    }

    /**
     * 获取服务组列表
     * @return
     */
    @GetMapping("/groups")
    @ResponseBody
    public ResponseVo<List<ServiceGroup>> getServiceGroup() {
        List<ServiceGroup> results = serviceManager.getServiceGroup();
        return HttpResponseUtils.success(results);
    }

    /**
     * 启动服务
     * @param services 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/startService")
    @ResponseBody
    @Permission
    public ResponseSimple startServer(@RequestBody List<String> services) {
        serviceManager.startService(services);
        return new ResponseSimple();
    }

    /**
     * 停止服务
     * @param services 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/stopService")
    @ResponseBody
    @Permission
    public ResponseSimple stopServer(@RequestBody List<String> services) {
        serviceManager.stopService(services);
        return new ResponseSimple();
    }

    /**
     * 重启服务
     * @param services 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/restartService")
    @ResponseBody
    @Permission
    public ResponseSimple restartServer(@RequestBody List<String> services) {
        serviceManager.restartService(services);
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
        serviceManager.oneClickRestart();
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
        serviceManager.oneClickStart();
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
        serviceManager.oneClickStop();
        return new ResponseSimple();
    }

    /**
     * base64编码
     * @param data 数据
     * @return 编码后的数据
     */
    @GetMapping(value="/base64Encoder")
    @ResponseBody
    public ResponseVo<String> base64Encoder(String data) {
        if (StringUtils.isEmpty(data)) {
            return new ResponseVo<>(ResultCodeConst.EMPTY_PARAM, "参数为空");
        }
        data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
        return new ResponseVo<>(data);
    }

    /**
     * 获取未被服务管理的JVM进程信息
     * @return 进程列表
     */
    @GetMapping(value="/jvmProcesses")
    @ResponseBody
    public ResponseVo<List<JvmProcess>> getJvmProcesses() {
        List<JvmProcess> results = serviceManager.getJvmProcesses();
        return HttpResponseUtils.success(results);
    }

    /**
     * attach进程
     * @param pid 进程PID
     * @return 执行结果
     */
    @GetMapping(value="/attach")
    @ResponseBody
    public ResponseSimple attach(String pid) {
        serviceManager.attach(pid);
        return new ResponseSimple();
    }

    /**
     * 删除服务
     * @param serviceName 服务名
     * @return 执行结果
     */
    @DeleteMapping(value="/service")
    @ResponseBody
    @Permission
    public ResponseSimple deleteServer(String serviceName) {
        serviceManager.deleteService(serviceName);
        return new ResponseSimple();
    }

    /**
     * 获取服务信息
     * @param serviceName 服务名
     * @return 服务信息
     */
    @GetMapping(value="/service")
    @ResponseBody
    public ResponseVo<ServiceInstance> getServer(String serviceName) {
        ServiceInstance result = serviceManager.getService(serviceName);
        return HttpResponseUtils.success(result);
    }
}
