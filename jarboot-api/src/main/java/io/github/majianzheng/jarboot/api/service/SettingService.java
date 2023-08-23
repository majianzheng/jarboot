package io.github.majianzheng.jarboot.api.service;

import io.github.majianzheng.jarboot.api.pojo.SystemSetting;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;

/**
 * 配置服务
 * @author majianzheng
 */
public interface SettingService {

    /**
     * 获取服务配置
     * @param serviceName 服务路径
     * @return 配置信息
     */
    ServiceSetting getServiceSetting(String serviceName);

    /**
     * 提交服务配置
     * @param setting 配置
     */
    void submitServiceSetting(ServiceSetting setting);

    /**
     * 获取全局配置
     * @return 配置
     */
    SystemSetting getSystemSetting();

    /**
     * 提交全局配置
     * @param setting 配置
     */
    void saveSetting(SystemSetting setting);

    /**
     * 获取vm options
     * @param serviceName 服务
     * @param file 文件
     * @return vm
     */
    String getVmOptions(String serviceName, String file);

    /**
     * 保存vm options
     * @param serviceName 服务
     * @param file 文件
     * @param content 文件内容
     */
    void saveVmOptions(String serviceName, String file, String content);
}
