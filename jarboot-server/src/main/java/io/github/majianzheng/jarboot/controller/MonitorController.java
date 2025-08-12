package io.github.majianzheng.jarboot.controller;

import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.monitor.MonitorService;
import io.github.majianzheng.jarboot.monitor.vo.Server;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 资源监控
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/monitor")
@RestController
public class MonitorController {
    @Resource
    MonitorService monitorService;

    /**
     * 获取服务器运行信息
     * @param clusterNode 集群节点
     * @return 服务器运行信息
     */
    @GetMapping(value = "/server")
    public ResponseVo<Server> getServerInfo(@RequestParam(required = false) String clusterNode) {
        if (CommonUtils.needProxy(clusterNode)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(clusterNode);
            return HttpResponseUtils.success(client.getServerInfo());
        }
        return HttpResponseUtils.success(monitorService.getServerInfo());
    }
}
