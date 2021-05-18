package com.mz.jarboot.service;

import com.mz.jarboot.dto.*;

import java.util.List;

public interface SettingService {
    /**
     * 获取日志文件列表
     * @return 配置文件列表
     */
    List<FileContentDTO> getLogFiles();

    String getFileContent(String path);
}
