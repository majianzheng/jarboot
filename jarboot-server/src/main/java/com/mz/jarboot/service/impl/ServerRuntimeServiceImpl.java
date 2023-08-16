package com.mz.jarboot.service.impl;

import com.mz.jarboot.api.pojo.ServerRuntimeInfo;
import com.mz.jarboot.common.utils.VersionUtils;
import com.mz.jarboot.service.ServerRuntimeService;
import com.mz.jarboot.utils.SettingUtils;
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
        return info;
    }

    @Override
    public String getUuid() {
        return SettingUtils.getUuid();
    }
}
