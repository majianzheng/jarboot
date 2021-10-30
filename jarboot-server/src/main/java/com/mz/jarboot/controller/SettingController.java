package com.mz.jarboot.controller;

import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.VersionUtils;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.service.SettingService;
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
     * @param path 服务路径
     * @return 服务配置
     */
    @GetMapping(value="/serverSetting")
    @ResponseBody
    @Permission("Get Server Setting")
    public ResponseForObject<ServerSetting> getServerSetting(String path) {
        try {
            ServerSetting results = settingService.getServerSetting(path);
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }

    /**
     * 提交服务配置
     * @param path 服务路径
     * @param setting 服务配置
     * @return
     */
    @PostMapping(value="/serverSetting")
    @ResponseBody
    @Permission("Submit Server Setting")
    public ResponseSimple submitServerSetting(@RequestParam String path,
                                              @RequestBody ServerSetting setting) {
        try {
            settingService.submitServerSetting(path, setting);
            return new ResponseSimple();
        } catch (JarbootException e) {
            return new ResponseSimple(e);
        }
    }

    /**
     * 获取全局配置
     * @return 全局配置
     */
    @GetMapping(value="/globalSetting")
    @ResponseBody
    @Permission("Get Global Setting")
    public ResponseForObject<GlobalSetting> getGlobalSetting() {
        try {
            GlobalSetting results = settingService.getGlobalSetting();
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
    @PostMapping(value="/globalSetting")
    @ResponseBody
    @Permission("Submit Global Setting")
    public ResponseSimple submitGlobalSetting(@RequestBody GlobalSetting setting) {
        try {
            settingService.submitGlobalSetting(setting);
            return new ResponseSimple();
        } catch (JarbootException e) {
            return new ResponseSimple(e);
        }
    }

    /**
     * 获取服务的VM配置
     * @param path 服务路径
     * @param file vm文件路径
     * @return vm配置
     */
    @GetMapping(value="/vmoptions")
    @ResponseBody
    @Permission("Get Server jvm options")
    public ResponseForObject<String> getVmOptions(String path, String file) {
        try {
            String results = settingService.getVmOptions(path, file);
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }

    /**
     * 保存服务的JVM配置
     * @param path 服务路径
     * @param file vm配置文件路径
     * @param content vm配置文件内容
     * @return 执行结果
     */
    @PostMapping(value="/vmoptions")
    @ResponseBody
    @Permission("Save Server jvm options")
    public ResponseSimple saveVmOptions(String path, String file, String content) {
        try {
            settingService.saveVmOptions(path, file, content);
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
