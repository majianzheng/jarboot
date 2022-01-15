package com.mz.jarboot.debug.plugin;

import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.api.service.ServiceManager;
import com.mz.jarboot.common.pojo.ResponseSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 调试服务插件
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/plugin/debug")
@RestController
public class DebugPluginController {
    @Autowired
    private ServiceManager serverMgrService;

    /**
     * 启动临时服务
     * @param setting 服务配置
     * @return 执行结果
     */
    @PostMapping("/startBySetting")
    @ResponseBody
    public ResponseSimple startBySetting(@RequestBody ServiceSetting setting) {
        if (null == setting.getWorkspace()) {
            setting.setWorkspace("");
        }
        serverMgrService.startSingleService(setting);
        return new ResponseSimple();
    }
}
