package com.mz.jarboot.controller;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.*;
import com.mz.jarboot.dto.*;
import com.mz.jarboot.service.ServerMgrService;
import com.mz.jarboot.ws.WebSocketManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Api(tags="服务管理")
@RequestMapping(value = "/jarboot-service", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class ServerMgrController {
    @Autowired
    private ServerMgrService serverMgrService;

    @ApiOperation(value = "获取服务列表", httpMethod = "GET")
    @GetMapping(value="/getServerList")
    @ResponseBody
    public ResponseForList<ServerRunningDTO> getWebServerList() {
        List<ServerRunningDTO> results = serverMgrService.getServerList();
        return new ResponseForList<>(results, results.size());
    }

    @ApiOperation(value = "启动服务", httpMethod = "POST")
    @PostMapping(value="/startServer")
    @ResponseBody
    public ResponseSimple startServer(@RequestBody List<String> servers) {
        serverMgrService.startServer(servers);
        return new ResponseSimple();
    }
    @ApiOperation(value = "停止服务", httpMethod = "POST")
    @PostMapping(value="/stopServer")
    @ResponseBody
    public ResponseSimple stopServer(@RequestBody List<String> servers) {
        serverMgrService.stopServer(servers);
        return new ResponseSimple();
    }
    @ApiOperation(value = "重启服务", httpMethod = "POST")
    @PostMapping(value="/restartServer")
    @ResponseBody
    public ResponseSimple restartServer(@RequestBody List<String> servers) {
        serverMgrService.restartServer(servers);
        return new ResponseSimple();
    }

    @ApiOperation(value = "一键重启", httpMethod = "GET")
    @GetMapping(value="/oneClickRestart")
    @ResponseBody
    public ResponseSimple oneClickRestart() {
        serverMgrService.oneClickRestart();
        return new ResponseSimple();
    }

    @ApiOperation(value = "一键启动", httpMethod = "GET")
    @GetMapping(value="/oneClickStart")
    @ResponseBody
    public ResponseSimple oneClickStart() {
        serverMgrService.oneClickStart();
        return new ResponseSimple();
    }

    @ApiOperation(value = "一键停止", httpMethod = "GET")
    @GetMapping(value="/oneClickStop")
    @ResponseBody
    public ResponseSimple oneClickStop() {
        serverMgrService.oneClickStop();
        return new ResponseSimple();
    }

    @ApiOperation(value = "执行命令", httpMethod = "POST")
    @PostMapping(value="/sendCommand")
    @ResponseBody
    public CommandResponse sendCommand(@RequestParam String server,
                                       @RequestBody Command command) {
        //检查是否attached
        CommandResponse resp = AgentManager.getInstance().sendCommandSync(server, command);
        if (ResultCodeConst.SUCCESS != resp.getResultCode()) {
            WebSocketManager.getInstance().sendOutMessage(server, resp.getResultMsg());
        }
        return resp;
    }

    @ApiOperation(hidden = true, value = "agent的应答", httpMethod = "POST")
    @PostMapping(value="/ack")
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
