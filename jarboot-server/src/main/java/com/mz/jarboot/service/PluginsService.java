package com.mz.jarboot.service;

import com.mz.jarboot.dto.PluginInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 插件管理
 * @author majianzheng
 */
public interface PluginsService {
    /**
     * 获取Agent插件列表
     * @return 插件列表
     */
    List<PluginInfoDTO> getAgentPlugins();

    /**
     * 更新或新增Agent插件
     * @param file 插件
     * @param type 类型
     */
    void uploadPlugin(MultipartFile file, String type);

    /**
     * 移除插件
     * @param type 类型，agent或server
     * @param filename 文件名
     */
    void removePlugin(String type, String filename);
}
