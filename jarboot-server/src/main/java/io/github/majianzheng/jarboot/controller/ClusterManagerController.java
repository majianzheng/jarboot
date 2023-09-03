package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.*;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientProxy;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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

    /**
     * 获取存活的集群
     * @return 集群列表
     */
    @GetMapping("onlineClusterHosts")
    @ResponseBody
    public ResponseVo<List<String>> getOnlineClusterHosts() {
        List<String> hosts = new ArrayList<>();
        ClusterClientManager.getInstance().getHosts().forEach((k, v) -> {
            if (v.isOnline()) {
                hosts.add(k);
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
     * @param instance 实例
     * @return
     */
    @PostMapping("deleteService")
    @ResponseBody
    public ResponseSimple deleteService(@RequestBody ServiceInstance instance) {
        clusterClientProxy.deleteService(instance);
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
}
