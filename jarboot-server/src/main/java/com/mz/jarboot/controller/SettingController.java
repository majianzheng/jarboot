package com.mz.jarboot.controller;

import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.VersionUtils;
import com.mz.jarboot.dto.*;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/setting")
@RestController
@Permission
public class SettingController {
    @Autowired
    private SettingService settingService;
    @Value("${docker:false}")
    private boolean isInDocker;

    /**
     * 获取服务配置
     * @param server 服务名
     * @return 服务配置
     */
    @GetMapping(value="/getServerSetting")
    @ResponseBody
    @Permission("Get Server Setting")
    public ResponseForObject<ServerSettingDTO> getServerSetting(String server) {
        try {
            ServerSettingDTO results = settingService.getServerSetting(server);
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }

    /**
     * 提交服务配置
     * @param server 服务名
     * @param setting 服务配置
     * @return
     */
    @PostMapping(value="/submitServerSetting")
    @ResponseBody
    @Permission("Submit Server Setting")
    public ResponseSimple submitServerSetting(@RequestParam String server,
                                              @RequestBody ServerSettingDTO setting) {
        try {
            settingService.submitServerSetting(server, setting);
            return new ResponseSimple();
        } catch (JarbootException e) {
            return new ResponseSimple(e);
        }
    }

    /**
     * 获取全局配置
     * @return 全局配置
     */
    @GetMapping(value="/getGlobalSetting")
    @ResponseBody
    @Permission("Get Global Setting")
    public ResponseForObject<GlobalSettingDTO> getGlobalSetting() {
        try {
            GlobalSettingDTO results = settingService.getGlobalSetting();
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }

    /**
     * 提交全局配置
     * @param setting 全局配置
     * @return 提交结果
     */
    @PostMapping(value="/submitGlobalSetting")
    @ResponseBody
    @Permission("Submit Global Setting")
    public ResponseSimple submitGlobalSetting(@RequestBody GlobalSettingDTO setting) {
        try {
            settingService.submitGlobalSetting(setting);
            return new ResponseSimple();
        } catch (JarbootException e) {
            return new ResponseSimple(e);
        }
    }

    /**
     * 获取服务的VM配置
     * @param server 服务名
     * @param file vm文件路径
     * @return vm配置
     */
    @GetMapping(value="/vmoptions")
    @ResponseBody
    @Permission("Get Server jvm options")
    public ResponseForObject<String> getVmOptions(String server, String file) {
        try {
            String results = settingService.getVmOptions(server, file);
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }

    /**
     * 保存服务的JVM配置
     * @param server 服务名
     * @param file vm配置文件路径
     * @param content vm配置文件内容
     * @return 执行结果
     */
    @PostMapping(value="/vmoptions")
    @ResponseBody
    @Permission("Save Server jvm options")
    public ResponseSimple saveVmOptions(String server, String file, String content) {
        try {
            settingService.saveVmOptions(server, file, content);
            return new ResponseSimple();
        } catch (JarbootException e) {
            return new ResponseSimple(e);
        }
    }

    /**
     * 获取版本
     * @return 版本
     */
    @GetMapping(value="/version")
    @ResponseBody
    public ResponseForObject<String> getVersion() {
        try {
            String results = "v" + VersionUtils.version;
            if (isInDocker) {
                results += "(Docker)";
            }
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }
}
