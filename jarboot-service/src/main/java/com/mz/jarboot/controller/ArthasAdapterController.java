package com.mz.jarboot.controller;

import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.service.ArthasAdapterService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Api(tags="使用第三方Arthas调试")
@RequestMapping(value = "/jarboot-arthas", method ={RequestMethod.GET, RequestMethod.POST})
@Controller
public class ArthasAdapterController {
    @Autowired
    private ArthasAdapterService arthasAdapterService;

    @ApiOperation(value = "检查是否安装类Arthas", httpMethod = "GET")
    @GetMapping (value="/checkArthasInstalled")
    @ResponseBody
    public ResponseSimple checkArthasInstalled() {
        arthasAdapterService.checkArthasInstalled();
        return new ResponseSimple();
    }

    @ApiOperation(value = "使用Arthas调试目标服务进程", httpMethod = "GET")
    @GetMapping(value="/attachToServer")
    @ResponseBody
    public ResponseSimple attachToServer(String server) {
        arthasAdapterService.attachToServer(server);
        return new ResponseSimple();
    }

    @ApiOperation(value = "获取当前使用Arthas调试的目标服务", httpMethod = "GET")
    @GetMapping(value="/getCurrentRunning")
    @ResponseBody
    public ResponseSimple getCurrentRunning() {
        arthasAdapterService.getCurrentRunning();
        return new ResponseSimple();
    }

    @ApiOperation(value = "停止Arthas调试", httpMethod = "GET")
    @GetMapping(value="/stopCurrentArthasInstance")
    @ResponseBody
    public ResponseSimple stopCurrentArthasInstance() {
        arthasAdapterService.stopCurrentArthasInstance();
        return new ResponseSimple();
    }

    @ApiOperation(hidden = true, value = "代理arthas界面", httpMethod = "GET")
    @GetMapping(value="/arthas")
    public void redirectToArthas(HttpServletResponse response) {
        try {
            response.sendRedirect("http://127.0.0.1:3658");
        } catch (IOException e) {
            //ignore
        }
    }
}
