package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.service.PluginsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping(value = "/jarboot/plugins")
@Controller
public class PluginsStaticController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private PluginsService pluginsService;

    /**
     * 加载插件页面
     * @param type 类型
     * @param plugin 插件
     * @param file 文件
     * @param response
     */
    @GetMapping("/page/{type}/{plugin}/{file}")
    @ResponseBody
    public void page(@PathVariable("type") String type,
                     @PathVariable("plugin") String plugin,
                     @PathVariable("file") String file,
                     HttpServletResponse response) {
        try (OutputStream outputStream = response.getOutputStream()) {
            pluginsService.readPluginStatic(type, plugin, file, outputStream);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Read plugin resource failed!");
            } catch (IOException exception) {
                //ignore
            }
        }
    }
}
