package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.PluginInfo;
import io.github.majianzheng.jarboot.service.PluginsService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author majianzheng
 */
@Service
public class PluginsServiceImpl implements PluginsService {
    private static final String AGENT_DIR = "agent";
    private static final String SERVER_DIR = "server";
    private static final String PLUGINS_DIR = "plugins";

    @Value("${JARBOOT_HOME:}")
    private String jarbootHome;

    @Override
    public List<PluginInfo> getAgentPlugins() {
        List<PluginInfo> result = new ArrayList<>();
        getPluginsByType(SERVER_DIR, result);
        getPluginsByType(AGENT_DIR, result);
        return result;
    }

    @Override
    public void uploadPlugin(MultipartFile file, String type) {
        String name = file.getOriginalFilename();
        if (null == name || !name.endsWith(CommonConst.JAR_EXT)) {
            return;
        }
        File dir = this.getPluginsDir(type);
        if (!dir.exists()) {
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                throw new JarbootException(e.getMessage(), e);
            }
        }

        try {
            file.transferTo(FileUtils.getFile(dir, name));
        } catch (IOException e) {
            throw new JarbootException("上传失败！" + file.getOriginalFilename(), e);
        }
    }

    @Override
    public void removePlugin(String type, String filename) {
        File file = FileUtils.getFile(this.jarbootHome + File.separator + PLUGINS_DIR + type, filename);
        try {
            FileUtils.delete(file);
        } catch (IOException e) {
            throw new JarbootException("Remove failed", e);
        }
    }

    @Override
    public void readPluginStatic(String type, String plugin, String filename, OutputStream outputStream) {
        if (!plugin.endsWith(CommonConst.JAR_EXT)) {
            plugin += CommonConst.JAR_EXT;
        }
        File file = getPluginFile(type, plugin);
        String resource = "page/" + filename;
        try (JarFile jarFile = new JarFile(file)){
            ZipEntry entry = jarFile.getEntry(resource);
            if (null == entry) {
                return;
            }
            try(InputStream is = jarFile.getInputStream(entry)) {
                byte[] buffer = new byte[2048];
                int size;
                while ((size = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, size);
                }
            }
        } catch (IOException e) {
            throw new JarbootException("Load plugin jar file failed!", e);
        }
    }

    private File getPluginFile(String type, String filename) {
        StringBuilder sb = new StringBuilder();
        sb
                .append(this.jarbootHome)
                .append(File.separator)
                .append(PLUGINS_DIR)
                .append(File.separator)
                .append(type);
        return FileUtils.getFile(sb.toString(), filename);
    }

    private void getPluginsByType(String type, List<PluginInfo> list) {
        File dir = this.getPluginsDir(type);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        Collection<File> files = FileUtils.listFiles(dir, new String[]{CommonConst.JAR_FILE_EXT}, false);
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        files.forEach(file -> {
            PluginInfo info = new PluginInfo();
            info.setId(list.size());
            String fileName = file.getName();
            int p = fileName.lastIndexOf("-plugin");
            if (-1 == p) {
                p = fileName.lastIndexOf(CommonConst.JAR_EXT);
            }
            info.setName(fileName.substring(0, p));
            info.setFileName(fileName);
            info.setLastModified(file.lastModified());
            info.setType(type);
            list.add(info);
        });
    }

    private File getPluginsDir(String type) {
        return FileUtils.getFile(this.jarbootHome + File.separator + PLUGINS_DIR, type);
    }
}
