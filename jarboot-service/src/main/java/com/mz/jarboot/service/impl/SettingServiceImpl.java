package com.mz.jarboot.service.impl;

import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.dto.FileContentDTO;
import com.mz.jarboot.exception.MzException;
import com.mz.jarboot.service.SettingService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class SettingServiceImpl implements SettingService {
    @Value("${root-path:}")
    private String ebrRootPath;

    @Value("${debug-mode:false}")
    private Boolean debugMode;

    /**
     * 获取日志文件列表
     *
     * @return 配置文件列表
     */
    @Override
    public List<FileContentDTO> getLogFiles() {
        List<FileContentDTO> logsList = new ArrayList<>();
        String userHome = System.getProperty("user.home");
        StringBuilder builder = new StringBuilder();
        builder.append(userHome).append(File.separator).
                append("ebr-workspace").append(File.separator).append("logs");
        String webLogDir = builder.toString();
        File logsDir = FileUtils.getFile(webLogDir);
        if (!logsDir.isDirectory()) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, String.format("未找到%s目录", webLogDir));
        }
        String[] extensions = {"log"};
        Collection<File> logs =  FileUtils.listFiles(logsDir, extensions, false);
        if (CollectionUtils.isNotEmpty(logs)) {
            logs.forEach(file -> {
                FileContentDTO contentDTO = new FileContentDTO();
                String fileName = file.getName();
                contentDTO.setFileName(fileName);
                contentDTO.setFilePath(file.getPath());
                contentDTO.setGroup("Web后端日志文件");
                logsList.add(contentDTO);
            });
        }
        return logsList;
    }

    @Override
    public String getFileContent(String path) {
        String content;
        try {
            content = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, e.getMessage(), e);
        }
        return content;
    }
}
