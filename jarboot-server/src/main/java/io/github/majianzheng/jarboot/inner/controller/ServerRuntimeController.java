package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
