package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.api.pojo.ServiceGroup;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.task.TaskRunCache;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 集群API服务
 * 仅限集群内部IP访问，保证集群内部使用统一的secret key
 * 普通界面登录使用的token禁用访问
 * @author mazheng
 */
@RequestMapping(value = CommonConst.CLUSTER_API_CONTEXT)
@RestController
//@PreAuthorize("hasRole('CLUSTER')")
public class ClusterApiController {
    @Autowired
    private TaskRunCache taskRunCache;
    @Autowired
    private ServiceManager serviceManager;
    @Autowired
    private ServerRuntimeService serverRuntimeService;
    @Autowired
    private SettingService settingService;

    @GetMapping("/check")
    @ResponseBody
    public ServerRuntimeInfo check() {
        return serverRuntimeService.getServerRuntimeInfo();
    }

    @GetMapping("/group")
    @ResponseBody
    public ServiceGroup getServiceGroup() {
        return taskRunCache.getServiceGroup(SettingUtils.getCurrentUserDir());
    }

    @GetMapping("/jvmGroup")
    @ResponseBody
    public ServiceGroup getJvmGroup() {
        return serviceManager.getJvmGroup();
    }

    @GetMapping("/serviceSetting")
    @ResponseBody
    public ServiceSetting getServiceSetting(String serviceName) {
        return settingService.getServiceSetting(serviceName);
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
}
