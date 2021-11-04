package com.mz.jarboot.task;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.VMUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
        server.forEach(item -> {
            if (AgentManager.getInstance().isOnline(item.getSid())) {
                item.setStatus(CommonConst.STATUS_RUNNING);
            }
            if (this.isStarting(item.getSid())) {
                item.setStatus(CommonConst.STATUS_STARTING);
            }
            if (this.isStopping(item.getSid())) {
                item.setStatus(CommonConst.STATUS_STOPPING);
            }
        });
    }

    public List<String> getServerPathList() {
        File[] serviceDirs = this.getServerDirs();
        List<String> paths = new ArrayList<>();
        for (File f : serviceDirs) {
            paths.add(f.getPath());
        }
        return paths;
    }

    public File[] getServerDirs() {
        String servicesPath = SettingUtils.getWorkspace();
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
            p.setStatus(CommonConst.STATUS_STOPPED);
            String path = f.getPath();
            p.setSid(SettingUtils.createSid(path));
            p.setPath(path);
            serverList.add(p);
        }
        updateServerInfo(serverList);
        return serverList;
    }

    public boolean hasStartingOrStopping() {
        return !this.startingCache.isEmpty() || !this.stoppingCache.isEmpty();
    }

    public boolean isStartingOrStopping(String sid) {
        return startingCache.containsKey(sid) || stoppingCache.containsKey(sid);
    }

    public boolean isStarting(String sid) {
        return startingCache.containsKey(sid);
    }

    public void addStarting(String sid) {
        startingCache.put(sid, System.currentTimeMillis());
    }

    public void removeStarting(String sid) {
        startingCache.remove(sid);
    }

    public boolean isStopping(String sid) {
        return stoppingCache.containsKey(sid);
    }

    public void addStopping(String sid) {
        stoppingCache.put(sid, System.currentTimeMillis());
    }

    public void removeStopping(String sid) {
        stoppingCache.remove(sid);
    }

    private boolean filterExcludeDir(File dir) {
        if (!dir.isDirectory() || dir.isHidden()) {
            return false;
        }
        final String name = dir.getName();
        if (StringUtils.startsWith(name, CommonConst.DOT)) {
            return false;
        }
        return !excludeDirSet.contains(name);
    }

    private void cleanPidFiles() {
        File logDir = FileUtils.getFile(SettingUtils.getLogDir());
        Collection<File> pidFiles = FileUtils.listFiles(logDir, new String[]{"pid"}, true);
        if (CollectionUtils.isNotEmpty(pidFiles)) {
            Map<Integer, String> allJvmPid = VMUtils.getInstance().listVM();
            pidFiles.forEach(file -> {
                try {
                    String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    int pid = NumberUtils.toInt(text, CommonConst.INVALID_PID);
                    if (allJvmPid.containsKey(pid)) {
                        return;
                    }
                } catch (Exception exception) {
                    //ignore
                }
                try {
                    FileUtils.forceDelete(file);
                } catch (Exception exception) {
                    //ignore
                }
            });
        }
    }

    @PostConstruct
    public void init() {
        //清理无效的pid文件
        this.cleanPidFiles();

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
