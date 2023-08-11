package com.mz.jarboot.service.impl;

import com.mz.jarboot.api.pojo.ServerRuntimeInfo;
import com.mz.jarboot.common.utils.VersionUtils;
import com.mz.jarboot.service.ServerRuntimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author mazheng
 */
@Service
public class ServerRuntimeServiceImpl implements ServerRuntimeService {
    private final String uuid = UUID.randomUUID().toString();
    @Value("${docker:false}")
    private boolean isInDocker;
    @Override
    public ServerRuntimeInfo getServerRuntimeInfo() {
        ServerRuntimeInfo info = new ServerRuntimeInfo();
        info.setUuid(uuid);
        info.setInDocker(isInDocker);
        info.setVersion(VersionUtils.version);
        return info;
    }

    @Override
    public String getUuid() {
        return uuid;
    }
}
