package com.mz.jarboot.task;

import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRunning;
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

    private void updateServerInfo(List<ServerRunning> server) {
        Map<String, Integer> pidCmdMap = TaskUtils.findProcess();
        server.forEach(item -> {
            Integer pid = pidCmdMap.remove(item.getName());
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
        // 如果不为空，则为自定义启动的服务
        if (!pidCmdMap.isEmpty()) {
            pidCmdMap.forEach((k, v) -> {
                ServerRunning serverRunning = new ServerRunning();
                serverRunning.setName(k);
                serverRunning.setStatus(CommonConst.STATUS_RUNNING);
                serverRunning.setPid(v);
                serverRunning.setEphemeral(true);
            });
        }
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
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, servicesPath + "目录不存在");
        }
        File[] serviceDirs = servicesDir.listFiles(this::filterExcludeDir);
        if (null == serviceDirs || serviceDirs.length < 1) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, servicesPath + "目录中不存在模块的服务");
        }
        // 根据名字排序
        Arrays.sort(serviceDirs, Comparator.comparing(File::getName));
        return serviceDirs;
    }

    public List<ServerRunning> getServerList() {
        List<ServerRunning> serverList = new ArrayList<>();
        File[] serviceDirs = getServerDirs();
        for (File f : serviceDirs) {
            String server = f.getName();
            ServerRunning p = new ServerRunning();
            p.setName(server);
            p.setEphemeral(false);
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
