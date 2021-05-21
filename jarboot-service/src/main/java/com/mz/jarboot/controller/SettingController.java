package com.mz.jarboot.controller;

import com.mz.jarboot.dto.*;
import com.mz.jarboot.service.SettingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags="系统配置")
@RequestMapping(value = "/jarboot-service", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class SettingController {
    @Autowired
    private SettingService settingService;

    @ApiOperation(value = "获取日志文件列表", httpMethod = "GET")
    @RequestMapping(value="/getLogFiles")
    @ResponseBody
    public ResponseForList<FileContentDTO> getLogFiles() {
        List<FileContentDTO> results = settingService.getLogFiles();
        return new ResponseForList<>(results, results.size());
    }

    @ApiOperation(value = "获取文件内容", httpMethod = "GET")
    @RequestMapping(value="/getFileContent")
    @ResponseBody
    public ResponseForObject<String> getFileContent(@RequestParam(name = "path") String path) {
        ResponseForObject<String> resp = new ResponseForObject<>();
        resp.setResult(settingService.getFileContent(path));
        return resp;
    }
}
