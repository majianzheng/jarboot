package com.mz.jarboot.service;

import com.mz.jarboot.dto.*;

/**
 * 配置服务
 * @author jianzhengma
 */
public interface SettingService {

    /**
     * 获取服务配置
     * @param server 服务
     * @return 配置信息
     */
    ServerSettingDTO getServerSetting(String server);

    /**
     * 提交服务配置
     * @param server 服务
     * @param setting 配置
     */
    void submitServerSetting(String server, ServerSettingDTO setting);

    /**
     * 获取全局配置
     * @return 配置
     */
    GlobalSettingDTO getGlobalSetting();

    /**
     * 提交全局配置
     * @param setting 配置
     */
    void submitGlobalSetting(GlobalSettingDTO setting);

    /**
     * 获取vm options
     * @param server 服务
     * @param file 文件
     * @return vm
     */
    String getVmOptions(String server, String file);

    /**
     * 保存vm options
     * @param server 服务
     * @param file 文件
     * @param content 文件内容
     */
    void saveVmOptions(String server, String file, String content);
}
