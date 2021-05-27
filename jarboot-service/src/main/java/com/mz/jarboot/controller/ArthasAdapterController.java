package com.mz.jarboot.controller;

import com.mz.jarboot.common.MzException;
import com.mz.jarboot.common.ResponseForObject;
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
    public ResponseForObject<Boolean> checkArthasInstalled() {
        try {
            boolean isInstalled = arthasAdapterService.checkArthasInstalled();
            return new ResponseForObject<>(isInstalled);
        } catch (MzException e) {
            return new ResponseForObject<>(e.getErrorCode(), e.getMessage());
        }
    }

    @ApiOperation(value = "使用Arthas调试目标服务进程", httpMethod = "GET")
    @GetMapping(value="/attachToServer")
    @ResponseBody
    public ResponseSimple attachToServer(String server) {
        try {
            arthasAdapterService.attachToServer(server);
            return new ResponseSimple();
        } catch (MzException e) {
            return new ResponseSimple(e.getErrorCode(), e.getMessage());
        }
    }

    @ApiOperation(value = "获取当前使用Arthas调试的目标服务", httpMethod = "GET")
    @GetMapping(value="/getCurrentRunning")
    @ResponseBody
    public ResponseForObject<String> getCurrentRunning() {
        try {
            String current = arthasAdapterService.getCurrentRunning();
            return new ResponseForObject<>(current);
        } catch (MzException e) {
            return new ResponseForObject<>(e.getErrorCode(), e.getMessage());
        }
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
