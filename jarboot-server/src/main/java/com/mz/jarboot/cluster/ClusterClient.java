package com.mz.jarboot.cluster;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServiceGroup;
import com.mz.jarboot.common.utils.HttpUtils;
import com.mz.jarboot.constant.AuthConst;
import com.mz.jarboot.utils.SettingUtils;

import javax.websocket.Session;
import java.util.HashMap;
import java.util.Map;

/**
 * 集群Api客户端
 * @author mazheng
 */
public class ClusterClient {
    private Session session;
    private String serverHost;

    public ServiceGroup getServiceGroup() {
        ServiceGroup group = HttpUtils.getObj(formatUrl(CommonConst.CLUSTER_CONTEXT + "/group"), ServiceGroup.class, wrapToken());
        group.setHost(serverHost);
        return group;
    }

    public ServiceGroup getJvmGroup() {
        ServiceGroup group = HttpUtils.getObj(formatUrl(CommonConst.CLUSTER_CONTEXT + "/jvmGroup"), ServiceGroup.class, wrapToken());
        group.setHost(serverHost);
        return group;
    }

    private String formatUrl(String api) {
        String url;
        final String http = "http";
        if (serverHost.startsWith(http)) {
            url = serverHost + api;
        } else {
            url = String.format("%s://%s%s", http, serverHost, api);
        }
        return url;
    }
    private Map<String, String> wrapToken() {
        String token = ClusterConfig.getInstance().getClusterToken(SettingUtils.getCurrentLoginUsername());
        Map<String, String> header = new HashMap<>(2);
        header.put(AuthConst.CLUSTER_TOKEN, token);
        return header;
    }
}
