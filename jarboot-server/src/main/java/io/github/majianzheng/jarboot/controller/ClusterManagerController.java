package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.*;
import io.github.majianzheng.jarboot.audit.ServiceInstanceFormat;
import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientProxy;
import io.github.majianzheng.jarboot.common.annotation.EnableAuditLog;
import io.github.majianzheng.jarboot.common.annotation.PrivilegeCheck;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 集群管理
 * @author mazheng
 */
@RequestMapping(value = CommonConst.CLUSTER_MGR_CONTEXT)
@RestController
@PrivilegeCheck(value = {"SERVICES_MGR", "ONLINE_DEBUG"})
public class ClusterManagerController {
    @Resource
    private ClusterClientProxy clusterClientProxy;
    @Resource
    private ServerRuntimeService serverRuntimeService;
    /**
     * 获取存活的集群
     * @return 集群列表
     */
    @GetMapping("onlineClusterHosts")
    public ResponseVo<List<HostInfo>> getOnlineClusterHosts() {
        List<HostInfo> hosts = new ArrayList<>();
        ClusterClientManager.getInstance().getHosts().forEach((k, v) -> {
            if (v.isOnline()) {
                HostInfo info = new HostInfo();
                info.setHost(v.getHost());
                info.setName(v.getName());
                info.setState(v.getState());
                hosts.add(info);
            }
        });
        return HttpResponseUtils.success(hosts);
    }

    /**
     * 获取服务组列表
     * @return 服务列表
     */
    @GetMapping("services")
    public ResponseVo<List<ServiceInstance>> getServiceGroup() {
        return HttpResponseUtils.success(clusterClientProxy.getServiceGroup());
    }

    /**
     * 获取JVM组列表
     * @return 服务列表
     */
    @GetMapping("jvmGroups")
    public ResponseVo<List<JvmProcess>> getJvmGroup() {
        return HttpResponseUtils.success(clusterClientProxy.getJvmGroup());
    }

    /**
     * 启动服务
     * @param services 服务实例
     * @return 执行结果
     */
    @PostMapping("startServices")
    @EnableAuditLog(value = "启动服务", argsFormat = ServiceInstanceFormat.class)
    public ResponseSimple startService(@RequestBody List<ServiceInstance> services) {
        clusterClientProxy.startService(services);
        return HttpResponseUtils.success();
    }

    /**
     * 停止服务
     * @param services 服务实例
     * @return 执行结果
     */
    @PostMapping("stopServices")
    @EnableAuditLog(value = "停止服务", argsFormat = ServiceInstanceFormat.class)
    public ResponseSimple stopService(@RequestBody List<ServiceInstance> services) {
        clusterClientProxy.stopService(services);
        return HttpResponseUtils.success();
    }

    /**
     * 重启服务
     * @param services 服务实例
     * @return 执行结果
     */
    @PostMapping("restartServices")
    @EnableAuditLog(value = "重启服务", argsFormat = ServiceInstanceFormat.class)
    public ResponseSimple restartService(@RequestBody List<ServiceInstance> services) {
        clusterClientProxy.restartService(services);
        return HttpResponseUtils.success();
    }

    /**
     * attach
     * @param host host
     * @param pid pid
     * @return 执行结果
     */
    @PostMapping("attach")
    @EnableAuditLog("attach进程")
    public ResponseSimple attach(String host, String pid) {
        clusterClientProxy.attach(host, pid);
        return HttpResponseUtils.success();
    }

    /**
     * 删除服务
     * @param instances 实例
     * @return 执行结果
     */
    @PostMapping("deleteService")
    @EnableAuditLog(value = "删除服务", argsFormat = ServiceInstanceFormat.class)
    public ResponseSimple deleteService(@RequestBody List<ServiceInstance> instances) {
        if (null != instances) {
            instances.forEach(instance -> clusterClientProxy.deleteService(instance));
        }
        return HttpResponseUtils.success();
    }

    /**
     * 获取服务配置
     * @param instance 实例
     * @return 服务配置
     */
    @PostMapping("serviceSetting")
    public ResponseVo<ServiceSetting> getServiceSetting(@RequestBody ServiceInstance instance) {
        return HttpResponseUtils.success(clusterClientProxy.getServiceSetting(instance));
    }

    /**
     * 保存服务配置
     * @param setting 实例
     * @return 服务配置
     */
    @PostMapping("saveServiceSetting")
    @EnableAuditLog(value = "保存服务配置")
    public ResponseVo<ServiceSetting> saveServiceSetting(@RequestBody ServiceSetting setting) {
        clusterClientProxy.saveServiceSetting(setting);
        return HttpResponseUtils.success();
    }


    /**
     * 导出服务
     * @param clusterHost 集群实例
     * @param name 服务名
     * @param response Servlet response
     * @throws IOException IO 异常
     */
    @GetMapping(value="/exportService/{name}.zip")
    @EnableAuditLog("导出服务")
    public void exportService(
            @RequestParam(required = false) String clusterHost,
            @PathVariable String name,
            HttpServletResponse response) throws IOException {
        CommonUtils.setDownloadHeader(response, name + ".zip");
        try (OutputStream os = response.getOutputStream()) {
            if (CommonUtils.needProxy(clusterHost)) {
                ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
                client.exportService(name, os);
            } else {
                serverRuntimeService.exportService(name, os);
            }
        }
    }

    /**
     * 导入服务
     * @param clusterHost 集群实例
     * @param file 文件
     * @return 执行结果
     */
    @PostMapping("/importService")
    @EnableAuditLog("导入服务")
    public ResponseVo<String> importService(
            @RequestParam(required = false) String clusterHost,
            @RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            if (CommonUtils.needProxy(clusterHost)) {
                ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
                client.importService(file.getOriginalFilename(), is);
            } else {
                serverRuntimeService.importService(file.getOriginalFilename(), is);
            }
        } catch (Exception e) {
            return HttpResponseUtils.error(e.getMessage());
        }
        return HttpResponseUtils.success();
    }

    /**
     * 从服务器下载文件
     * @param clusterHost 集群实例
     * @param file base64编码的文件全路径名
     * @param response Servlet response
     */
    @GetMapping(value="/download")
    @EnableAuditLog("从服务器下载文件")
    public void download(
            @RequestParam(name = "clusterHost", required = false) String clusterHost,
            @RequestParam(name = "file") String file, HttpServletResponse response) throws IOException {
        CommonUtils.setDownloadHeader(response, null);
        try (OutputStream os = response.getOutputStream()) {
            if (CommonUtils.needProxy(clusterHost)) {
                ClusterClient client = ClusterClientManager.getInstance().getClient(clusterHost);
                client.downloadAnyFile(file, os);
            } else {
                serverRuntimeService.downloadAnyFile(file, os);
            }
        }
    }
}
