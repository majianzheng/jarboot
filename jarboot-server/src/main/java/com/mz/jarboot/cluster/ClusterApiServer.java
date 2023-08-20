package com.mz.jarboot.cluster;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRuntimeInfo;
import com.mz.jarboot.api.pojo.ServiceGroup;
import com.mz.jarboot.api.service.ServiceManager;
import com.mz.jarboot.service.ServerRuntimeService;
import com.mz.jarboot.task.TaskRunCache;
import com.mz.jarboot.utils.SettingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * 集群API服务
 * 仅限集群内部IP访问，保证集群内部使用统一的secret key
 * 普通界面登录使用的token禁用访问
 * @author mazheng
 */
@RequestMapping(value = CommonConst.CLUSTER_CONTEXT)
@RestController
@PreAuthorize("hasRole('CLUSTER')")
public class ClusterApiServer {
    @Autowired
    private TaskRunCache taskRunCache;
    @Autowired
    private ServiceManager serviceManager;
    @Autowired
    private ServerRuntimeService serverRuntimeService;

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
        ServiceGroup localGroup = new ServiceGroup();
        localGroup.setHost(ClusterConfig.getInstance().getSelfHost());
        localGroup.setChildren(new ArrayList<>());
        localGroup.getChildren().addAll(serviceManager.getJvmProcesses());
        return localGroup;
    }
}
