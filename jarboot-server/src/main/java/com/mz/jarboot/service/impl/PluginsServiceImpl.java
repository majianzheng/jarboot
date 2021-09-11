package com.mz.jarboot.service.impl;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.PluginInfoDTO;
import com.mz.jarboot.service.PluginsService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author majianzheng
 */
@Service
public class PluginsServiceImpl implements PluginsService {
    private static final String AGENT_DIR = "agent";
    private static final String PLUGINS_DIR = "plugins";

    @Value("${jarboot.home:}")
    private String jarbootHome;

    @Override
    public List<PluginInfoDTO> getAgentPlugins() {
        List<PluginInfoDTO> result = new ArrayList<>();
        File dir = this.getPluginsDir(AGENT_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return result;
        }
        Collection<File> files = FileUtils.listFiles(dir, CommonConst.JAR_FILE_EXT, false);
        if (CollectionUtils.isEmpty(files)) {
            return result;
        }
        files.forEach(file -> {
            PluginInfoDTO info = new PluginInfoDTO();
            String fileName = file.getName();
            int p = fileName.lastIndexOf("-plugin");
            if (-1 == p) {
                p = fileName.lastIndexOf(".jar");
            }
            info.setName(fileName.substring(0, p));
            info.setFileName(fileName);
            info.setLastModified(file.lastModified());
            info.setType(AGENT_DIR);
            result.add(info);
        });
        return result;
    }

    @Override
    public void uploadPlugin(MultipartFile file, String type) {
        File dir = this.getPluginsDir(type);
        if (!dir.exists()) {
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                throw new JarbootException(e.getMessage(), e);
            }
        }
        String name = file.getOriginalFilename();
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

    private File getPluginsDir(String type) {
        return FileUtils.getFile(this.jarbootHome + File.separator + PLUGINS_DIR, type);
    }
}
