package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.auth.annotation.Permission;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.pojo.ResponseForList;
import com.mz.jarboot.common.pojo.ResponseForObject;
import com.mz.jarboot.common.pojo.ResponseSimple;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.service.SettingService;
import com.mz.jarboot.utils.SettingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 系统配置
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.SETTING_CONTEXT)
@RestController
@Permission
public class SettingController {
    @Autowired
    private SettingService settingService;

    /**
     * 获取服务配置
     * @param serviceName 服务路径
     * @return 服务配置
     */
    @GetMapping(value="/serviceSetting")
    @ResponseBody
    @Permission("Get Server Setting")
    public ResponseForObject<ServiceSetting> getServerSetting(String serviceName) {
        try {
            ServiceSetting results = settingService.getServiceSetting(serviceName);
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }

    /**
     * 提交服务配置
     * @param setting 服务配置
     */
    @PostMapping(value="/serviceSetting")
    @ResponseBody
    @Permission("Submit Server Setting")
    public ResponseSimple submitServerSetting(@RequestBody ServiceSetting setting) {
        try {
            settingService.submitServiceSetting(setting);
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
     * @param serviceName 服务路径
     * @param file vm文件路径
     * @return vm配置
     */
    @GetMapping(value="/vmoptions")
    @ResponseBody
    @Permission("Get Server jvm options")
    public ResponseForObject<String> getVmOptions(String serviceName, String file) {
        try {
            String results = settingService.getVmOptions(serviceName, file);
            return new ResponseForObject<>(results);
        } catch (JarbootException e) {
            return new ResponseForObject<>(e);
        }
    }

    /**
     * 保存服务的JVM配置
     * @param serviceName 服务路径
     * @param file vm配置文件路径
     * @param content vm配置文件内容
     * @return 执行结果
     */
    @PostMapping(value="/vmoptions")
    @ResponseBody
    @Permission("Save Server jvm options")
    public ResponseSimple saveVmOptions(String serviceName, String file, String content) {
        try {
            settingService.saveVmOptions(serviceName, file, content);
            return new ResponseSimple();
        } catch (JarbootException e) {
            return new ResponseSimple(e);
        }
    }

    @PostMapping(value="/trustedHost")
    @ResponseBody
    @Permission("Add trusted host")
    public ResponseSimple addTrustedHost(String host) throws IOException {
        SettingUtils.addTrustedHost(host);
        AgentManager.getInstance().addTrustedHost(host);
        return new ResponseSimple();
    }

    @DeleteMapping(value="/trustedHost")
    @ResponseBody
    @Permission("Delete trusted host")
    public ResponseSimple removeTrustedHost(String host) throws IOException {
        SettingUtils.removeTrustedHost(host);
        return new ResponseSimple();
    }

    @GetMapping(value="/trustedHost")
    @ResponseBody
    public ResponseForList<String> getTrustedHosts() {
        return new ResponseForList<>(new ArrayList<>(SettingUtils.getTrustedHosts()));
    }
}
