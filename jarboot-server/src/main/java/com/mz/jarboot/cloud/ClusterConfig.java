package com.mz.jarboot.cloud;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRuntimeInfo;
import com.mz.jarboot.common.utils.HttpUtils;
import com.mz.jarboot.service.ServerRuntimeService;
import com.mz.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 集群配置
 * @author mazheng
 */
@Component
public class ClusterConfig {
    @Autowired
    private ServerRuntimeService runtimeService;
    /** 集群列表 */
    private Set<String> hosts = new HashSet<>(16);
    /** 主节点 */
    private String masterHost;

    public Set<String> getHosts() {
        return hosts;
    }

    public String getMasterHost() {
        return masterHost;
    }

    private File getClusterConfigFile() {
        return FileUtils.getFile(SettingUtils.getHomePath(), "conf", "cluster.conf");
    }

    private ServerRuntimeInfo getServerInfo(String host) {
        String url;
        if (host.startsWith("http")) {
            url = host + CommonConst.SERVER_RUNTIME_CONTEXT;
        } else {
            url = String.format("http://%s%s", host, CommonConst.SERVER_RUNTIME_CONTEXT);
        }
        return HttpUtils.getObj(url, ServerRuntimeInfo.class);
    }

    public void init() {
        final String notePrefix = "#";
        try {
            List<String> lines = FileUtils.readLines(getClusterConfigFile(), StandardCharsets.UTF_8);
            if (CollectionUtils.isEmpty(lines)) {
                return;
            }
            lines.forEach(line -> {
                line = line.trim();
                if (line.startsWith(notePrefix)) {
                    return;
                }
                // 检查host合法性
                hosts.add(line);
            });
        } catch (Exception e) {
            // ignore
        }
    }
}
