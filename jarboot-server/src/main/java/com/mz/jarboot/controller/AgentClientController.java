package com.mz.jarboot.controller;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseSimple;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 内部接口，与jarboot-core交互，非开放
 * @author majianzheng
 */
@RequestMapping(value = "/api/public/agent")
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
    public ResponseSimple ack(@RequestParam String server, @RequestBody String raw) {
        CommandResponse resp = CommandResponse.createFromRaw(raw);
        AgentManager.getInstance().handleAgentResponse(server, resp);
        return new ResponseSimple();
    }
}
