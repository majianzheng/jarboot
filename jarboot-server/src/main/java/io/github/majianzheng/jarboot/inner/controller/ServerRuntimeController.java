package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.ZipUtils;
import io.github.majianzheng.jarboot.service.ServerRuntimeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * jarboot服务运行时信息
 * @author mazheng
 */
@RequestMapping(value = CommonConst.SERVER_RUNTIME_CONTEXT)
@RestController
public class ServerRuntimeController {
    @Resource
    private ServerRuntimeService serverRuntimeService;

    @GetMapping
    public ServerRuntimeInfo getServerRuntimeInfo() {
        return serverRuntimeService.getServerRuntimeInfo();
    }

    @GetMapping("client-tools.zip")
    public void downloadCli(HttpServletResponse response) throws IOException {
        final String clientToolsTempDir = "jarboot-cli-tools";
        File tempDir = FileUtils.getFile(CacheDirHelper.getCacheDir(), clientToolsTempDir);

        try (OutputStream os = response.getOutputStream()){
            // 初始化临时目录
            final String windows = "windows";
            final String[] dirs = new String[]{"bin" + File.separator + windows, CommonConst.COMPONENTS_NAME + File.separator + "lib", "plugins" + File.separator + "agent"};
            for (String dir : dirs) {
                FileUtils.forceMkdir(FileUtils.getFile(tempDir, dir));
            }
            // 拷贝客户端脚本
            final String[] bashFiles = new String[]{"client-cli", "jt"};
            for (String bashFile : bashFiles) {
                File srcFile = FileUtils.getFile(SettingUtils.getHomePath(), "bin", bashFile + ".sh");
                File dstFile = FileUtils.getFile(tempDir, "bin", bashFile + ".sh");
                FileUtils.copyFile(srcFile, dstFile);
                // 拷贝windows脚本到bin/windows目录
                File winFile = FileUtils.getFile(SettingUtils.getHomePath(), "bin", windows, bashFile + ".cmd");
                File winDstFile = FileUtils.getFile(tempDir, "bin", windows, bashFile + ".cmd");
                FileUtils.copyFile(winFile, winDstFile);
            }
            File commonSrcFile = FileUtils.getFile(SettingUtils.getHomePath(), "bin", "common.sh");
            File commonDstFile = FileUtils.getFile(tempDir, "bin", "common.sh");
            FileUtils.copyFile(commonSrcFile, commonDstFile);
            // 拷贝jar包
            final String[] jarFiles = new String[]{"jarboot-agent.jar", "jarboot-core.jar", "jarboot-spy.jar", "jarboot-tools.jar"};
            for (String jar : jarFiles) {
                File srcFile = FileUtils.getFile(SettingUtils.getHomePath(), CommonConst.COMPONENTS_NAME, jar);
                File dstFile = FileUtils.getFile(tempDir, CommonConst.COMPONENTS_NAME, jar);
                FileUtils.copyFile(srcFile, dstFile);
            }
            // 拷贝依赖
            Set<String> paths = getCliToolDependencies();
            for (String path : paths) {
                if (path.endsWith(CommonConst.JAR_EXT)) {
                    File libFile = FileUtils.getFile(SettingUtils.getHomePath(), CommonConst.COMPONENTS_NAME, path);
                    File dstFile = FileUtils.getFile(tempDir, CommonConst.COMPONENTS_NAME, path);
                    FileUtils.copyFile(libFile, dstFile);
                }
            }
            ZipUtils.toZip(tempDir, os, true);
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private Set<String> getCliToolDependencies() {
        Set<String> dependencies = new HashSet<>(16);
        final String[] parseFiles = new String[]{"jarboot-core.jar", "jarboot-tools.jar"};
        for (String fileName : parseFiles) {
            File file = FileUtils.getFile(SettingUtils.getHomePath(), CommonConst.COMPONENTS_NAME, fileName);
            Set<String> paths = CommonUtils.getJarDependencies(file);
            if (!CollectionUtils.isEmpty(paths)) {
                dependencies.addAll(paths);
            }
        }
        return dependencies;
    }
}
