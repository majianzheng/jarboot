package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.utils.VersionUtils;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author mazheng
 */
@Service
public class ServerRuntimeServiceImpl implements ServerRuntimeService {
    @Value("${docker:false}")
    private boolean isInDocker;
    @Override
    public ServerRuntimeInfo getServerRuntimeInfo() {
        ServerRuntimeInfo info = new ServerRuntimeInfo();
        info.setUuid(SettingUtils.getUuid());
        info.setInDocker(isInDocker);
        info.setVersion(VersionUtils.version);
        info.setHost(ClusterClientManager.getInstance().getSelfHost());
        return info;
    }

    @Override
    public String getUuid() {
        return SettingUtils.getUuid();
    }
}
