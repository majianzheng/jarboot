package com.mz.jarboot.task;

import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.ServerRunningDTO;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author majianzheng
 */
@Component
public class TaskRunCache {
    @Value("${jarboot.services.exclude-dirs:bin,lib,conf,plugins,plugin}")
    private String excludeDirs;
    private final HashSet<String> excludeDirSet = new HashSet<>(16);
    private final ConcurrentHashMap<String, Long> startingCache = new ConcurrentHashMap<>(16);
    private final ConcurrentHashMap<String, Long> stoppingCache = new ConcurrentHashMap<>(16);

    private void updateServerInfo(List<ServerRunningDTO> server) {
        Map<String, Integer> pidCmdMap = TaskUtils.findJavaProcess();
        server.forEach(item -> {
            Integer pid = pidCmdMap.getOrDefault(item.getName(), -1);
            if (null == pid || CommonConst.INVALID_PID == pid) {
                item.setStatus(CommonConst.STATUS_STOPPED);
                return;
            }
            item.setPid(pid);
            item.setStatus(CommonConst.STATUS_RUNNING);
            if (startingCache.containsKey(item.getName())) {
                item.setStatus(CommonConst.STATUS_STARTING);
            }
            if (stoppingCache.containsKey(item.getName())) {
                item.setStatus(CommonConst.STATUS_STOPPING);
            }
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

    public boolean hasStartingOrStopping() {
        return !this.startingCache.isEmpty() || !this.stoppingCache.isEmpty();
    }

    public boolean isStartingOrStopping(String server) {
        return this.isStarting(server) || this.isStopping(server);
    }

    public boolean isStarting(String server) {
        return startingCache.containsKey(server);
    }

    public void addStarting(String server) {
        startingCache.put(server, System.currentTimeMillis());
    }

    public void removeStarting(String server) {
        startingCache.remove(server);
    }

    public boolean isStopping(String server) {
        return stoppingCache.containsKey(server);
    }

    public void addStopping(String server) {
        stoppingCache.put(server, System.currentTimeMillis());
    }

    public void removeStopping(String server) {
        stoppingCache.remove(server);
    }

    private boolean filterExcludeDir(File dir, String name) {
        if (!dir.isDirectory() || dir.isHidden()) {
            return false;
        }
        if (StringUtils.startsWith(name, CommonConst.DOT)) {
            return false;
        }
        return !excludeDirSet.contains(name);
    }

    @PostConstruct
    public void init() {
        if (StringUtils.isBlank(excludeDirs)) {
            return;
        }
        String[] dirs = excludeDirs.split(CommonConst.COMMA_SPLIT);
        for (String s : dirs) {
            if (StringUtils.isNotBlank(s)) {
                s = StringUtils.trim(s);
                excludeDirSet.add(s);
            }
        }
    }
}
