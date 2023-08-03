package com.mz.jarboot.service;

import com.mz.jarboot.api.pojo.ServiceSetting;

/**
 * @author majianzheng
 */
public interface TaskWatchService {
    /**
     * 初始化监控
     */
    void init();

    /**
     * 注册文件文件变动监控
     * @param setting
     */
    void registerServiceChangeMonitor(ServiceSetting setting);

    /**
     * 取消注册文件变动监控
     * @param sid
     */
    void unregisterServiceChangeMonitor(String sid);
}
