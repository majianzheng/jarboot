package com.mz.jarboot.inner.controller;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRuntimeInfo;
import com.mz.jarboot.service.ServerRuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * jarboot服务运行时信息
 * @author mazheng
 */
@RequestMapping(value = CommonConst.SERVER_RUNTIME_CONTEXT)
@Controller
public class ServerRuntimeController {
    @Autowired
    private ServerRuntimeService serverRuntimeService;

    @GetMapping
    @ResponseBody
    public ServerRuntimeInfo getServerRuntimeInfo() {
        return serverRuntimeService.getServerRuntimeInfo();
    }
}
