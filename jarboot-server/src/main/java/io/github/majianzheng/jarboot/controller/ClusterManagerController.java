package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.*;
import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientProxy;
import io.github.majianzheng.jarboot.cluster.ClusterServerState;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ClusterManagerController {
    @Resource
    private ClusterClientProxy clusterClientProxy;
    @Autowired
    private ServerRuntimeService serverRuntimeService;
    /**
     * 获取存活的集群
     * @return 集群列表
     */
    @GetMapping("onlineClusterHosts")
    @ResponseBody
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
    @ResponseBody
    public ResponseVo<List<ServiceInstance>> getServiceGroup() {
        return HttpResponseUtils.success(clusterClientProxy.getServiceGroup());
    }

    /**
     * 获取JVM组列表
     * @return 服务列表
     */
    @GetMapping("jvmGroups")
    @ResponseBody
    public ResponseVo<List<JvmProcess>> getJvmGroup() {
        return HttpResponseUtils.success(clusterClientProxy.getJvmGroup());
    }

    /**
     * 启动服务
     * @param services 服务实例
     * @return
     */
    @PostMapping("startServices")
    @ResponseBody
    public ResponseSimple startService(@RequestBody List<ServiceInstance> services) {
        clusterClientProxy.startService(services);
        return HttpResponseUtils.success();
    }

    /**
     * 停止服务
     * @param services 服务实例
     * @return
     */
    @PostMapping("stopServices")
    @ResponseBody
    public ResponseSimple stopService(@RequestBody List<ServiceInstance> services) {
        clusterClientProxy.stopService(services);
        return HttpResponseUtils.success();
    }

    /**
     * 重启服务
     * @param services 服务实例
     * @return
     */
    @PostMapping("restartServices")
    @ResponseBody
    public ResponseSimple restartService(@RequestBody List<ServiceInstance> services) {
        clusterClientProxy.restartService(services);
        return HttpResponseUtils.success();
    }

    /**
     * attach
     * @param host host
     * @param pid pid
     * @return
     */
    @PostMapping("attach")
    @ResponseBody
    public ResponseSimple attach(String host, String pid) {
        clusterClientProxy.attach(host, pid);
        return HttpResponseUtils.success();
    }

    /**
     * 删除服务
     * @param instances 实例
     * @return
     */
    @PostMapping("deleteService")
    @ResponseBody
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
    @ResponseBody
    public ResponseVo<ServiceSetting> getServiceSetting(@RequestBody ServiceInstance instance) {
        return HttpResponseUtils.success(clusterClientProxy.getServiceSetting(instance));
    }

    /**
     * 保存服务配置
     * @param setting 实例
     * @return 服务配置
     */
    @PostMapping("saveServiceSetting")
    @ResponseBody
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
    @GetMapping(value="/exportService")
    public void exportService(
            @RequestParam(required = false) String clusterHost,
            @RequestParam String name,
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
    @ResponseBody
    public ResponseVo<String> importService(@RequestParam(required = false) String clusterHost, @RequestParam("file") MultipartFile file) {
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
    @GetMapping(value="/download/{file}")
    public void download(@RequestParam(required = false) String clusterHost, @PathVariable("file") String file, HttpServletResponse response) throws IOException {
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

    public static class HostInfo {
        private String host;
        private String name;
        private ClusterServerState state;
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ClusterServerState getState() {
            return state;
        }

        public void setState(ClusterServerState state) {
            this.state = state;
        }
    }
}
