package com.mz.jarboot.controller;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseForObject;
import com.mz.jarboot.common.ResponseSimple;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 内部接口，与jarboot-core交互，非开放
 * @author majianzheng
 */
@RequestMapping(value = "/api/jarboot/public/agent")
@Controller
public class AgentClientController {

    /**
     * 命令执行结果反馈接口
     * @param server 服务名
     * @param raw 执行结果数据
     * @return 处理结果
     */
    @PostMapping(value="/response")
    @ResponseBody
    public ResponseSimple onResponse(@RequestParam String server, @RequestParam String sid, @RequestBody String raw) {
        CommandResponse resp = CommandResponse.createFromRaw(raw);
        AgentManager.getInstance().handleAgentResponse(server, sid, resp, null);
        return new ResponseSimple();
    }

    /**
     * 通知启动成功完成
     * @param server 服务名
     * @return 处理结果
     */
    @GetMapping(value="/setStarted")
    @ResponseBody
    public ResponseSimple setStarted(@RequestParam String server, @RequestParam String sid) {
        AgentManager.getInstance().onServerStarted(server, sid);
        return new ResponseSimple();
    }

    /**
     * 获取连接的IP地址
     * @param request 请求
     * @return IP地址
     */
    @PostMapping(value="/remoteAddr")
    @ResponseBody
    public ResponseForObject<String> remoteAddr(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        return new ResponseForObject<>(remote);
    }
}
