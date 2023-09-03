package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.JvmProcess;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
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
    public ResponseVo<ServiceInstance> getServiceGroup() {
        return HttpResponseUtils.success(serviceManager.getServiceGroup());
    }

    /**
     * 获取JVM组列表
     * @return
     */
    @GetMapping("/jvmGroups")
    @ResponseBody
    public ResponseVo<JvmProcess> getJvmGroup() {
        return HttpResponseUtils.success(serviceManager.getJvmGroup());
    }

    /**
     * 启动服务
     * @param services 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/startService")
    @ResponseBody
    public ResponseSimple startServer(@RequestBody List<String> services) {
        serviceManager.startService(services);
        return HttpResponseUtils.success();
    }

    /**
     * 停止服务
     * @param services 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/stopService")
    @ResponseBody
    public ResponseSimple stopServer(@RequestBody List<String> services) {
        serviceManager.stopService(services);
        return HttpResponseUtils.success();
    }

    /**
     * 重启服务
     * @param services 服务列表
     * @return 执行结果
     */
    @PostMapping(value="/restartService")
    @ResponseBody
    public ResponseSimple restartServer(@RequestBody List<String> services) {
        serviceManager.restartService(services);
        return HttpResponseUtils.success();
    }

    /**
     * 启动单个服务
     * @param setting 服务配置
     * @return
     */
    @PostMapping(value="/startSingleService")
    @ResponseBody
    public ResponseSimple startSingleService(@RequestBody ServiceSetting setting) {
        serviceManager.startSingleService(setting);
        return HttpResponseUtils.success();
    }

    /**
     * 停止单个服务
     * @param setting 服务配置
     * @return
     */
    @PostMapping(value="/stopSingleService")
    @ResponseBody
    public ResponseSimple stopSingleService(@RequestBody ServiceSetting setting) {
        serviceManager.stopSingleService(setting);
        return HttpResponseUtils.success();
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
        return HttpResponseUtils.success(data);
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
        return HttpResponseUtils.success();
    }

    /**
     * 删除服务
     * @param serviceName 服务名
     * @return 执行结果
     */
    @DeleteMapping(value="/service")
    @ResponseBody
    public ResponseSimple deleteServer(String serviceName) {
        serviceManager.deleteService(serviceName);
        return HttpResponseUtils.success();
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
