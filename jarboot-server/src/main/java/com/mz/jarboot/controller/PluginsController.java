package com.mz.jarboot.controller;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.ResponseForList;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.api.pojo.PluginInfo;
import com.mz.jarboot.service.PluginsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 插件管理
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/plugins")
@RestController
@Permission
public class PluginsController {
    @Autowired
    private PluginsService pluginsService;

    /**
     * 上传插件文件
     * @param file 文件
     * @return 执行结果
     */
    @PostMapping
    @ResponseBody
    @Permission("Add plugin")
    public ResponseSimple uploadPlugin(@RequestParam("file") MultipartFile file,
                                       @RequestParam("type") String type) {
        pluginsService.uploadPlugin(file, type);
        return new ResponseSimple();
    }

    /**
     * 获取插件列表
     * @return 执行结果
     */
    @GetMapping
    @ResponseBody
    public ResponseForList<PluginInfo> getAgentPlugins() {
        return new ResponseForList<>(pluginsService.getAgentPlugins());
    }

    /**
     * 移除插件
     * @param type 插件路径
     * @param filename 文件名
     * @return 执行结果
     */
    @DeleteMapping
    @ResponseBody
    @Permission("Remove plugin")
    public ResponseSimple removePlugin(String type, String filename) {
        pluginsService.removePlugin(type, filename);
        return new ResponseSimple();
    }
}
