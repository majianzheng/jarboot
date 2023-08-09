package com.mz.jarboot.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.SystemSetting;
import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.pojo.ResponseVo;
import com.mz.jarboot.common.pojo.ResponseSimple;
import com.mz.jarboot.api.service.SettingService;
import com.mz.jarboot.common.utils.HttpResponseUtils;
import com.mz.jarboot.utils.SettingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Set;

/**
 * 系统配置
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.SETTING_CONTEXT)
@RestController
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
    public ResponseVo<ServiceSetting> getServerSetting(String serviceName) {
        ServiceSetting results = settingService.getServiceSetting(serviceName);
        return new ResponseVo<>(results);
    }

    /**
     * 提交服务配置
     * @param setting 服务配置
     */
    @PostMapping(value="/serviceSetting")
    @ResponseBody
    public ResponseSimple submitServerSetting(@RequestBody ServiceSetting setting) {
        settingService.submitServiceSetting(setting);
        return new ResponseSimple();
    }

    /**
     * 获取全局配置
     * @return 全局配置
     */
    @GetMapping(value="/globalSetting")
    @ResponseBody
    public ResponseVo<SystemSetting> getGlobalSetting() {
        SystemSetting results = settingService.getSystemSetting();
        return new ResponseVo<>(results);
    }

    /**
     * 提交全局配置
     * @param setting 全局配置
     * @return 提交结果
     */
    @PostMapping(value="/globalSetting")
    @ResponseBody
    public ResponseSimple submitGlobalSetting(@RequestBody SystemSetting setting) {
        settingService.saveSetting(setting);
        return new ResponseSimple();
    }

    /**
     * 获取服务的VM配置
     * @param serviceName 服务路径
     * @param file vm文件路径
     * @return vm配置
     */
    @GetMapping(value="/vmoptions")
    @ResponseBody
    public ResponseVo<String> getVmOptions(String serviceName, String file) {
        String results = settingService.getVmOptions(serviceName, file);
        return new ResponseVo<>(results);
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
    public ResponseSimple saveVmOptions(String serviceName, String file, String content) {
        settingService.saveVmOptions(serviceName, file, content);
        return new ResponseSimple();
    }

    /**
     * 增加信任主机
     * @param host 主机
     * @return
     * @throws IOException
     */
    @PostMapping(value="/trustedHost")
    @ResponseBody
    public ResponseSimple addTrustedHost(String host) throws IOException {
        SettingUtils.addTrustedHost(host);
        AgentManager.getInstance().addTrustedHost(host);
        return new ResponseSimple();
    }

    /**
     * 移除信任主机
     * @param host 主机
     * @return
     * @throws IOException
     */
    @DeleteMapping(value="/trustedHost")
    @ResponseBody
    public ResponseSimple removeTrustedHost(String host) throws IOException {
        SettingUtils.removeTrustedHost(host);
        return new ResponseSimple();
    }

    /**
     * 获取信任主机
     * @return 信任主机列表
     */
    @GetMapping(value="/trustedHost")
    @ResponseBody
    public ResponseVo<Set<String>> getTrustedHosts() {
        return HttpResponseUtils.success(SettingUtils.getTrustedHosts());
    }
}
