package com.mz.jarboot.task;

import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.ServerRunningDTO;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;

@Component
public class TaskRunCache {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private TaskRunFile taskRunFile = null;

    @Value("${jarboot.services.exclude-dirs:bin,lib,conf,plugins,plugin}")
    private String excludeDirs;
    private HashSet<String> excludeDirSet = new HashSet<>(16);

    private void updateServerInfo(List<ServerRunningDTO> server) {
        Map<String, Integer> pidCmdMap = TaskUtils.findJavaProcess();
        server.forEach(item -> {
            Integer pid = pidCmdMap.getOrDefault(item.getName(), -1);
            String status = this.getTaskStatus(item.getName());
            if (null == pid || CommonConst.INVALID_PID == pid) {
                item.setStatus(CommonConst.STATUS_STOPPED);
                return;
            }
            item.setPid(pid);
            //未发现ip和端口配置时的运行中的判定
            Date actionTime = this.getActionTime(item.getName());
            //点击开始超过60秒，或jarboot重启过时，存在pid则判定为已经启动
            if (null == actionTime || ((System.currentTimeMillis() - actionTime.getTime()) > 60000)) {
                item.setStatus(CommonConst.STATUS_RUNNING);
                return;
            }
            item.setStatus(status);
        });
    }

    public List<String> getServerNameList() {
        File[] serviceDirs = this.getServerDirs();
        List<String> allWebServerList = new ArrayList<>();
        for (File f : serviceDirs) {
            String server = f.getName();
            allWebServerList.add(server);
        }
        return allWebServerList;
    }

    public File[] getServerDirs() {
        String servicesPath = SettingUtils.getServicesPath();
        File servicesDir = new File(servicesPath);
        if (!servicesDir.isDirectory() || !servicesDir.exists()) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, servicesPath + "目录不存在");
        }
        File[] serviceDirs = servicesDir.listFiles(this::filterExcludeDir);
        if (null == serviceDirs || serviceDirs.length < 1) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, servicesPath + "目录中不存在模块的服务");
        }
        return serviceDirs;
    }

    public List<ServerRunningDTO> getServerList() {
        List<ServerRunningDTO> serverList = new ArrayList<>();
        File[] serviceDirs = getServerDirs();
        for (File f : serviceDirs) {
            String server = f.getName();
            ServerRunningDTO p = new ServerRunningDTO();
            p.setName(server);
            serverList.add(p);
        }
        updateServerInfo(serverList);
        return serverList;
    }

    public void setTaskInfo(String name, String status, Integer pid) {
        TaskRunFile cache = this.load();
        cache.setTaskInfo(name, status, pid);
        this.update(cache);
    }

    public String getTaskStatus(String name) {
        return this.load().getTaskStatus(name);
    }
    public boolean hasNotFinished() {
        return this.load().hasNotFinished();
    }

    public Date getTaskStartedTime(String name) {
        return this.load().getTaskStartTime(name);
    }

    public Integer getTaskPid(String name) {
        return this.load().getTaskPid(name);
    }

    public Date getActionTime(String name) {
        return this.load().getActionTime(name);
    }

    private TaskRunFile load() {
        if (null != this.taskRunFile) {
            return this.taskRunFile;
        }
        TaskRunFile cache = new TaskRunFile();
        File cacheFile = FileUtils.getFile(this.getCacheFilePath());
        if (!cacheFile.exists() || !cacheFile.isFile()) {
            return cache;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
            cache = (TaskRunFile) ois.readObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return cache;
    }

    private boolean filterExcludeDir(File dir, String name) {
        if (!dir.isDirectory() || dir.isHidden()) {
            return false;
        }
        if (StringUtils.startsWith(name, ".")) {
            return false;
        }
        return !excludeDirSet.contains(name);
    }

    private void update(TaskRunFile cache) {
        File cacheFile = FileUtils.getFile(this.getCacheFilePath());
        try (ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(cacheFile));){
            if (cacheFile.isDirectory()) {
                FileUtils.deleteDirectory(cacheFile);
            }
            oo.writeObject(cache);
            //再更新到内存
            this.taskRunFile = cache;
        } catch (Exception e) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, "更新缓存文件失败！", e);
        }
    }

    private String getCacheFilePath() {
        String path = System.getProperty(CommonConst.JARBOOT_HOME);
        StringBuilder builder = new StringBuilder();
        builder.append(path).append(File.separator).append("logs").append(File.separator).append("taskRun.temp");
        return builder.toString();
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(excludeDirs)) {
            return;
        }
        String[] dirs = excludeDirs.split(",");
        for (String s : dirs) {
            s = StringUtils.trim(s);
            if (StringUtils.isNoneBlank(s)) {
                excludeDirSet.add(s);
            }
        }
    }
}
