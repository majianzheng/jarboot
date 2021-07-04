package com.mz.jarboot.controller;

import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.dto.*;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.service.SettingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Api(tags="系统配置")
@RequestMapping(value = "/api/jarboot-setting", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class SettingController {
    @Autowired
    private SettingService settingService;

    @ApiOperation(value = "获取服务配置", httpMethod = "GET")
    @GetMapping(value="/getServerSetting")
    @ResponseBody
    public ResponseForObject<ServerSettingDTO> getServerSetting(String server) {
        try {
            ServerSettingDTO results = settingService.getServerSetting(server);
            return new ResponseForObject<>(results);
        } catch (MzException e) {
            return new ResponseForObject<>(e);
        }
    }

    @ApiOperation(value = "提交服务配置", httpMethod = "POST")
    @PostMapping(value="/submitServerSetting")
    @ResponseBody
    public ResponseSimple submitServerSetting(@RequestParam String server,
                                              @RequestBody ServerSettingDTO setting) {
        try {
            settingService.submitServerSetting(server, setting);
            return new ResponseSimple();
        } catch (MzException e) {
            return new ResponseSimple(e);
        }
    }

    @ApiOperation(value = "获取全局配置", httpMethod = "GET")
    @GetMapping(value="/getGlobalSetting")
    @ResponseBody
    public ResponseForObject<GlobalSettingDTO> getGlobalSetting() {
        try {
            GlobalSettingDTO results = settingService.getGlobalSetting();
            return new ResponseForObject<>(results);
        } catch (MzException e) {
            return new ResponseForObject<>(e);
        }
    }

    @ApiOperation(value = "提交全局配置", httpMethod = "POST")
    @PostMapping(value="/submitGlobalSetting")
    @ResponseBody
    public ResponseSimple submitGlobalSetting(@RequestBody GlobalSettingDTO setting) {
        try {
            settingService.submitGlobalSetting(setting);
            return new ResponseSimple();
        } catch (MzException e) {
            return new ResponseSimple(e);
        }
    }
}
