package com.mz.jarboot.service;

/**
 * @author majianzheng
 */
public interface TaskWatchService {
    /**
     * 初始化监控
     */
    void init();

    /**
     * 工作空间改变
     * @param workspace 工作空间
     */
    void changeWorkspace(String workspace);
}
