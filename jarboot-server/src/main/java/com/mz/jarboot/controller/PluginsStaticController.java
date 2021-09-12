package com.mz.jarboot.controller;

import com.mz.jarboot.service.PluginsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 插件界面静态资源访问
 * @author majianzheng
 */
@RequestMapping(value = "/plugins")
@Controller
public class PluginsStaticController {
    @Autowired
    private PluginsService pluginsService;

    @GetMapping("/page/{type}/{plugin}/{file}")
    @ResponseBody
    public void page(@PathVariable("type") String type,
                     @PathVariable("plugin") String plugin,
                     @PathVariable("file") String file,
                     HttpServletResponse response) {
        try (OutputStream outputStream = response.getOutputStream();) {
            pluginsService.readPluginStatic(type, plugin, file, outputStream);
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException exception) {
                //ignore
            }
        }
    }
}
