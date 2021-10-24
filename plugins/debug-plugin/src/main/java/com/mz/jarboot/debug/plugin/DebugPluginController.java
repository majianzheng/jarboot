package com.mz.jarboot.debug.plugin;

import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.api.service.ServerMgrService;
import com.mz.jarboot.common.ResponseSimple;
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
    private ServerMgrService serverMgrService;

    /**
     * 启动临时服务
     * @param setting 服务配置
     * @return 执行结果
     */
    @PostMapping("/startServer")
    @ResponseBody
    public ResponseSimple startSingleServer(@RequestBody ServerSetting setting) {
        serverMgrService.startSingleServer(setting);
        return new ResponseSimple();
    }
}
