package com.mz.jarboot.controller;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseSimple;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Api(tags="代理客户端开放接口（非开放，内部使用）", hidden = true)
@RequestMapping(value = "/api/agent", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class AgentClientController {

    @ApiOperation(hidden = true, value = "agent的推送消息", httpMethod = "POST")
    @PostMapping(value="/response")
    @ResponseBody
    public ResponseSimple ack(@RequestParam String server, @RequestBody String raw) {
        CommandResponse resp = CommandResponse.createFromRaw(raw);
        AgentManager.getInstance().handleAgentResponse(server, resp);
        return new ResponseSimple();
    }
}
