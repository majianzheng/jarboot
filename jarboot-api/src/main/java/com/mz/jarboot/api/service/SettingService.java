package com.mz.jarboot.api.service;

import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServerSetting;

/**
 * 配置服务
 * @author majianzheng
 */
public interface SettingService {

    /**
     * 获取服务配置
     * @param path 服务路径
     * @return 配置信息
     */
    ServerSetting getServerSetting(String path);

    /**
     * 提交服务配置
     * @param path 服务
     * @param setting 配置
     */
    void submitServerSetting(String path, ServerSetting setting);

    /**
     * 获取全局配置
     * @return 配置
     */
    GlobalSetting getGlobalSetting();

    /**
     * 提交全局配置
     * @param setting 配置
     */
    void submitGlobalSetting(GlobalSetting setting);

    /**
     * 获取vm options
     * @param path 服务路径
     * @param file 文件
     * @return vm
     */
    String getVmOptions(String path, String file);

    /**
     * 保存vm options
     * @param server 服务
     * @param file 文件
     * @param content 文件内容
     */
    void saveVmOptions(String server, String file, String content);
}
