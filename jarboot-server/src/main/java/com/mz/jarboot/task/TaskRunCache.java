package com.mz.jarboot.task;

import com.mz.jarboot.common.ConcurrentWeakKeyHashMap;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dao.TaskRunInfoDao;
import com.mz.jarboot.dto.ServerRunningDTO;
import com.mz.jarboot.entity.TaskRunInfo;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;

@Component
public class TaskRunCache {
    @Autowired
    private TaskRunInfoDao taskRunInfoDao;
    @Value("${jarboot.services.exclude-dirs:bin,lib,conf,plugins,plugin}")
    private String excludeDirs;
    private HashSet<String> excludeDirSet = new HashSet<>(16);
    // 使用内存缓存，提升效率，防止每次都去数据库中寻找
    private final ConcurrentWeakKeyHashMap<String, TaskRunInfo> taskMap = new ConcurrentWeakKeyHashMap<>();

    private void updateServerInfo(List<ServerRunningDTO> server) {
        Map<String, Integer> pidCmdMap = TaskUtils.findJavaProcess();
        server.forEach(item -> {
            Integer pid = pidCmdMap.getOrDefault(item.getName(), -1);
            TaskRunInfo taskRunInfo = this.getTaskRunInfo(item.getName());
            String status = taskRunInfo.getStatus();
            if (null == pid || CommonConst.INVALID_PID == pid) {
                item.setStatus(CommonConst.STATUS_STOPPED);
                return;
            }
            item.setPid(pid);
            //未发现ip和端口配置时的运行中的判定
            long actionTime = taskRunInfo.getLastUpdateTime();
            //点击开始超过60秒，或jarboot重启过时，存在pid则判定为已经启动
            if (((System.currentTimeMillis() - actionTime) > 60000)) {
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
        // 根据名字排序
        Arrays.sort(serviceDirs, Comparator.comparing(File::getName));
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

    @Transactional
    public void setTaskInfo(String name, String status, Integer pid) {
        TaskRunInfo taskRunInfo = taskRunInfoDao.findFirstByName(name);
        if (null == taskRunInfo) {
            taskRunInfo = new TaskRunInfo();
            taskRunInfo.setName(name);
        }
        taskRunInfo.setStatus(status);
        taskRunInfo.setPid(pid);
        taskRunInfo.setLastUpdateTime(System.currentTimeMillis());
        taskRunInfoDao.save(taskRunInfo);
        taskMap.put(name, taskRunInfo);
    }

    private TaskRunInfo getTaskRunInfo(final String name) {
        TaskRunInfo taskRunInfo = taskMap.getOrDefault(name, null);
        if (null != taskRunInfo) {
            return taskRunInfo;
        }
        synchronized (name) { // NOSONAR
            taskRunInfo = taskRunInfoDao.findFirstByName(name);
            if (null == taskRunInfo) {
                taskRunInfo = new TaskRunInfo();
                taskRunInfo.setName(name);
                taskRunInfo.setStatus(CommonConst.STATUS_STOPPED);
                taskRunInfo.setLastUpdateTime(System.currentTimeMillis());
            }
            taskMap.put(name, taskRunInfo);
        }
        return taskRunInfo;
    }

    public String getTaskStatus(final String name) {
        return getTaskRunInfo(name).getStatus();
    }

    public Integer getTaskPid(String name) {
        return this.getTaskRunInfo(name).getPid();
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
