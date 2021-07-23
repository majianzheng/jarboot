package com.mz.jarboot.service;

/**
 * @author jianzhengma
 */
public interface TaskWatchService {
    /**
     * 初始化监控
     */
    void init();

    /**
     * attach到服务
     * @param server 服务
     */
    void attachServer(String server);
}
