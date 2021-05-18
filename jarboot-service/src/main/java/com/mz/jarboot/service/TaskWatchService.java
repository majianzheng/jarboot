package com.mz.jarboot.service;

public interface TaskWatchService {
    /**
     * 是否启用路径监控
     * @param enabled 是否启用
     */
    void enablePathWatch(Boolean enabled);

    /**
     * 添加要守护的服务
     * @param serviceName 服务名
     */
    void addDaemonService(String serviceName);

    /**
     * 移除要守护的服务
     * @param serviceName 服务名
     */
    void removeDaemonService(String serviceName);
}
