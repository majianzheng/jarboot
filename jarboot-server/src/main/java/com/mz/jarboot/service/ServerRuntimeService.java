package com.mz.jarboot.service;

import com.mz.jarboot.api.pojo.ServerRuntimeInfo;

/**
 * @author mazheng
 */
public interface ServerRuntimeService {
    /**
     * 获取Jarboot运行时信息
     * @return 运行时信息
     */
    ServerRuntimeInfo getServerRuntimeInfo();

    /**
     * 获取UUID
     * @return uuid
     */
    String getUuid();
}
