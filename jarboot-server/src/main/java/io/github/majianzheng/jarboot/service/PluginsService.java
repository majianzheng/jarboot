package io.github.majianzheng.jarboot.service;

import io.github.majianzheng.jarboot.api.pojo.PluginInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
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
    List<PluginInfo> getAgentPlugins();

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

    /**
     * 获取插件静态资源
     * @param type 类型
     * @param plugin 插件文件名
     * @param file 资源文件名
     * @param outputStream 输出流
     */
    void readPluginStatic(String type, String plugin, String file, OutputStream outputStream);
}
