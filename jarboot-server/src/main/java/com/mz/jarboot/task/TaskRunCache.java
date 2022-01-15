package com.mz.jarboot.task;

import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.CacheDirHelper;
import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.common.utils.VMUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
    /** 需要排除的工作空间里的目录 */
    @Value("${jarboot.services.exclude-dirs:bin,lib,conf,plugins,plugin}")
    private String excludeDirs;
    @Autowired
    private TaskStatusChangeSubscriber taskStatusChangeSubscriber;
    /** 需要排除的工作空间里的目录 */
    private final HashSet<String> excludeDirSet = new HashSet<>(16);
    /** 正在启动中的服务 */
    private final ConcurrentHashMap<String, Long> startingCache = new ConcurrentHashMap<>(16);
    /** 正在停止中的服务 */
    private final ConcurrentHashMap<String, Long> stoppingCache = new ConcurrentHashMap<>(16);

    /**
     * 获取服务路径列表
     * @return 服务路径列表
     */
    public List<String> getServicePathList() {
        File[] serviceDirs = this.getServiceDirs();
        List<String> paths = new ArrayList<>();
        if (null != serviceDirs && serviceDirs.length > 0) {
            for (File f : serviceDirs) {
                paths.add(f.getPath());
            }
        }
        return paths;
    }

    /**
     * 获取服务目录列表
     * @return 服务目录
     */
    public File[] getServiceDirs() {
        String servicesPath = SettingUtils.getWorkspace();
        File servicesDir = new File(servicesPath);
        if (!servicesDir.isDirectory() || !servicesDir.exists()) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, servicesPath + "目录不存在");
        }
        File[] serviceDirs = servicesDir.listFiles(this::filterExcludeDir);
        if (null == serviceDirs || serviceDirs.length < 1) {
            return serviceDirs;
        }
        // 根据名字排序
        Arrays.sort(serviceDirs, Comparator.comparing(File::getName));
        return serviceDirs;
    }

    public ServiceInstance getService(File serverDir) {
        ServiceInstance process = new ServiceInstance();
        process.setName(serverDir.getName());
        String path = serverDir.getPath();
        String sid = SettingUtils.createSid(path);
        process.setSid(sid);
        process.setPath(path);
        process.setGroup(this.getGroup(sid, path));

        if (this.isStarting(sid)) {
            process.setStatus(CommonConst.STARTING);
        } else if (this.isStopping(sid)) {
            process.setStatus(CommonConst.STOPPING);
        } else if (AgentManager.getInstance().isOnline(sid)) {
            process.setStatus(CommonConst.RUNNING);
        } else {
            process.setStatus(CommonConst.STOPPED);
        }
        return process;
    }

    /**
     * 获取服务列表
     * @return 服务列表
     */
    public List<ServiceInstance> getServiceList() {
        List<ServiceInstance> serverList = new ArrayList<>();
        File[] serviceDirs = getServiceDirs();
        if (null == serviceDirs || serviceDirs.length <= 0) {
            return serverList;
        }
        for (File file : serviceDirs) {
            ServiceInstance process = getService(file);
            serverList.add(process);
        }
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

    public boolean addStarting(String sid) {
        return null == startingCache.putIfAbsent(sid, System.currentTimeMillis());
    }

    public void removeStarting(String sid) {
        startingCache.remove(sid);
    }

    public boolean isStopping(String sid) {
        return stoppingCache.containsKey(sid);
    }

    public boolean addStopping(String sid) {
        return null == stoppingCache.putIfAbsent(sid, System.currentTimeMillis());
    }

    public void removeStopping(String sid) {
        stoppingCache.remove(sid);
    }

    private String getGroup(String sid, String path) {
        ServiceSetting setting = PropertyFileUtils.getServerSettingBySid(sid);
        if (null != setting) {
            return setting.getGroup();
        }
        setting = PropertyFileUtils.getServerSettingByPath(path);
        if (null == setting) {
            return StringUtils.EMPTY;
        }
        return setting.getGroup();
    }

    private boolean filterExcludeDir(File dir) {
        if (!dir.isDirectory() || dir.isHidden()) {
            return false;
        }
        final String name = dir.getName();
        if (name.startsWith(CommonConst.DOT)) {
            return false;
        }
        if (StringUtils.containsWhitespace(name)) {
            return false;
        }
        return !excludeDirSet.contains(name);
    }

    private void cleanPidFiles() {
        File pidDir = CacheDirHelper.getPidDir();
        if (!pidDir.exists()) {
            return;
        }
        if (!pidDir.isDirectory()) {
            try {
                FileUtils.forceDelete(pidDir);
            } catch (Exception e) {
                //ignore
            }
            return;
        }
        Collection<File> pidFiles = FileUtils.listFiles(pidDir, new String[]{"pid"}, true);
        if (!CollectionUtils.isEmpty(pidFiles)) {
            Map<String, String> allJvmPid = VMUtils.getInstance().listVM();
            pidFiles.forEach(file -> {
                try {
                    String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    if (allJvmPid.containsKey(text)) {
                        return;
                    }
                } catch (Exception exception) {
                    //ignore
                }
                FileUtils.deleteQuietly(file);
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
            if (!StringUtils.isBlank(s)) {
                excludeDirSet.add(s.trim());
            }
        }
        NotifyReactor.getInstance().registerSubscriber(taskStatusChangeSubscriber);
    }
}
