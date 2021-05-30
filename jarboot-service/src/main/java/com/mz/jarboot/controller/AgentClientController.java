package com.mz.jarboot.controller;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResponseType;
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
    public ResponseSimple ack(@RequestParam String server, @RequestBody String raw) {
        CommandResponse resp = CommandResponse.createFromRaw(raw);
        ResponseType type = resp.getResponseType();
        switch (type) {
            case ACK:
                AgentManager.getInstance().onAck(server, resp);
                break;
            case CONSOLE:
                WebSocketManager.getInstance().sendOutMessage(server, resp.getBody());
                break;
            case COMPLETE:
                WebSocketManager.getInstance().commandComplete(server, resp.getCmd());
                break;
            default:
                break;
        }
        return new ResponseSimple();
    }
}
