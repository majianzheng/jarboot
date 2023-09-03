package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.*;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.cluster.ClusterEventMessage;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.task.TaskRunCache;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 集群API服务
 * 仅限集群内部IP访问，保证集群内部使用统一的secret key
 * 普通界面登录使用的token禁用访问
 * @author mazheng
 */
@RequestMapping(value = CommonConst.CLUSTER_API_CONTEXT)
@RestController
@PreAuthorize("hasRole('CLUSTER')")
public class ClusterApiController {
    @Autowired
    private TaskRunCache taskRunCache;
    @Autowired
    private ServiceManager serviceManager;
    @Autowired
    private ServerRuntimeService serverRuntimeService;
    @Autowired
    private SettingService settingService;

    @GetMapping("/health")
    @ResponseBody
    public ServerRuntimeInfo health() {
        return serverRuntimeService.getServerRuntimeInfo();
    }

    @GetMapping("/group")
    @ResponseBody
    public ServiceInstance getServiceGroup() {
        return taskRunCache.getServiceGroup(SettingUtils.getCurrentUserDir());
    }

    @GetMapping("/jvmGroup")
    @ResponseBody
    public JvmProcess getJvmGroup() {
        return serviceManager.getJvmGroup();
    }

    @GetMapping("/serviceSetting")
    @ResponseBody
    public ServiceSetting getServiceSetting(String serviceName) {
        return settingService.getServiceSetting(serviceName);
    }

    @PostMapping("/serviceSetting")
    @ResponseBody
    public ResponseSimple saveServiceSetting(@RequestBody ServiceSetting setting) {
        settingService.submitServiceSetting(setting);
        return HttpResponseUtils.success();
    }

    @DeleteMapping("/service")
    @ResponseBody
    public ResponseSimple deleteService(String serviceName) {
        serviceManager.deleteService(serviceName);
        return HttpResponseUtils.success();
    }

    @GetMapping("/attach")
    @ResponseBody
    public ResponseSimple attach(String pid) {
        serviceManager.attach(pid);
        return HttpResponseUtils.success();
    }

    @PostMapping("/handleMessage/{host}")
    @ResponseBody
    public ResponseSimple handleMessage(@RequestBody ClusterEventMessage eventMessage, @PathVariable("host") String host) {
        ClusterClient client = ClusterClientManager.getInstance().getClient(host);
        if (null == client) {
            return HttpResponseUtils.error("集群客户端不存在" + host);
        }
        client.handleMessage(eventMessage);
        return HttpResponseUtils.success();
    }
}
