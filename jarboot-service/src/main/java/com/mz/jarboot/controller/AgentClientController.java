package com.mz.jarboot.controller;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.ws.WebSocketManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Api(tags="代理客户端开放接口")
@RequestMapping(value = "/api/agent", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class AgentClientController {

    @ApiOperation(hidden = true, value = "agent的推送消息", httpMethod = "POST")
    @PostMapping(value="/response")
    @ResponseBody
    public ResponseSimple ack(@RequestParam String server, @RequestBody CommandResponse resp) {
        switch (resp.getType()) {
            case CommandConst.ACK_TYPE:
                AgentManager.getInstance().onAck(server, resp);
                break;
            case CommandConst.CONSOLE_TYPE:
                WebSocketManager.getInstance().sendOutMessage(server, resp.getBody());
                break;
            default:
                break;
        }
        return new ResponseSimple();
    }
}
