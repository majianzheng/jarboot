package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.UpgradeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;

/**
 * jarboot服务运行时信息
 * @author mazheng
 */
@RequestMapping(value = "/api/jarboot/upgrade")
@RestController
public class UpgradeController {
    @Resource
    private UpgradeService upgradeService;
    /**
     * 上传安装包升级
     * @param file 文件
     * @return 执行结果
     */
    @PostMapping("upload")
    public ResponseSimple upload(
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            // 上传服务文件
            upgradeService.upgrade(file.getOriginalFilename(), is);
        }
        return HttpResponseUtils.success();
    }

    /**
     * 从url下载安装包升级
     * @param url 下载的url
     * @return 执行结果
     */
    @PostMapping("url")
    public ResponseSimple upload(
            @RequestParam(value = "url", required = false) String url) {
        upgradeService.upgrade(url);
        return HttpResponseUtils.success();
    }
}
