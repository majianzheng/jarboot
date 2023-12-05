package io.github.majianzheng.jarboot.task;

import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.notify.AbstractEventRegistry;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.utils.PropertyFileUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.ws.WebSocketMainServer;
import io.github.majianzheng.jarboot.common.utils.VMUtils;
import org.apache.commons.io.FileUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author majianzheng
 */
@Component
public class TaskRunCache {
    private final Logger logger = LoggerFactory.getLogger(TaskRunCache.class);
    /** 需要排除的工作空间里的目录 */
    @Value("${jarboot.services.exclude-dirs:bin,lib,conf,plugins,plugin}")
    private String excludeDirs;
    @Autowired
    private AbstractEventRegistry eventRegistry;
    @Resource
    private Scheduler scheduler;

    /** 需要排除的工作空间里的目录 */
    private final HashSet<String> excludeDirSet = new HashSet<>(16);
    /** 正在启动中的服务 */
    private final ConcurrentHashMap<String, Long> startingCache = new ConcurrentHashMap<>(16);
    /** 正在停止中的服务 */
    private final ConcurrentHashMap<String, Long> stoppingCache = new ConcurrentHashMap<>(16);

    /**
     * 获取服务名称列表
     * @return 服务名称列表
     */
    public List<String> getServiceNameList(String username) {
        File[] serviceDirs = this.getServiceDirs(username);
        List<String> paths = new ArrayList<>();
        if (null != serviceDirs) {
            for (File f : serviceDirs) {
                paths.add(f.getName());
            }
        }
        return paths;
    }

    /**
     * 获取服务目录列表
     * @return 服务目录
     */
    public File[] getServiceDirs(String userDir) {
        String workspace = SettingUtils.getWorkspace();
        File servicesDir = FileUtils.getFile(workspace, userDir);
        checkUserDir(servicesDir);
        File[] serviceDirs = servicesDir.listFiles(this::filterExcludeDir);
        if (null == serviceDirs || serviceDirs.length < 1) {
            return serviceDirs;
        }
        // 根据名字排序
        Arrays.sort(serviceDirs, Comparator.comparing(File::getName));
        return serviceDirs;
    }

    public ServiceInstance getService(String userDir, File serverDir) {
        ServiceInstance instance = new ServiceInstance();
        instance.setName(serverDir.getName());
        instance.setHost(ClusterClientManager.getInstance().getSelfHost());
        instance.setHostName(ClusterClientManager.getInstance().getSelfHostName());
        String path = serverDir.getAbsolutePath();
        String sid = SettingUtils.createSid(path);
        instance.setSid(sid);
        instance.setGroup(this.getGroup(userDir, instance.getName(), path));

        if (this.isStarting(sid)) {
            instance.setStatus(CommonConst.STARTING);
        } else if (this.isStopping(sid)) {
            instance.setStatus(CommonConst.STOPPING);
        } else if (isScheduling(sid)) {
            instance.setStatus(CommonConst.SCHEDULING);
        } else if (AgentManager.getInstance().isOnline(sid)) {
            instance.setStatus(CommonConst.RUNNING);
        } else {
            instance.setStatus(CommonConst.STOPPED);
        }
        return instance;
    }

    public boolean isScheduling(String sid) {
        try {
            if (scheduler.checkExists(TriggerKey.triggerKey(sid))) {
                return true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 获取服务列表
     * @param userDir 用户目录
     * @return 服务列表
     */
    public List<ServiceInstance> getServiceList(String userDir) {
        List<ServiceInstance> serverList = new ArrayList<>();
        File[] serviceDirs = getServiceDirs(userDir);
        if (null == serviceDirs) {
            return serverList;
        }
        for (File file : serviceDirs) {
            ServiceInstance process = getService(userDir, file);
            serverList.add(process);
        }
        return serverList;
    }

    /**
     * 获取服务组
     * @param userDir 用户目录
     * @return 服务组
     */
    public ServiceInstance getServiceGroup(String userDir) {
        List<ServiceInstance> serviceList = this.getServiceList(userDir);
        ServiceInstance localGroup = new ServiceInstance();
        final String selfHost = ClusterClientManager.getInstance().getSelfHost();
        final String selfHostName = ClusterClientManager.getInstance().getSelfHostName();
        localGroup.setNodeType(CommonConst.NODE_ROOT);
        localGroup.setSid(String.format("%08x", SettingUtils.getUuid().hashCode()));
        localGroup.setHost(selfHost);
        localGroup.setHostName(selfHostName);
        localGroup.setChildren(new ArrayList<>());
        if (CollectionUtils.isEmpty(serviceList)) {
            return localGroup;
        }
        HashMap<String, ServiceInstance> map = new HashMap<>(16);
        List<ServiceInstance> list = new ArrayList<>();
        serviceList.forEach(service -> {
            if (StringUtils.isEmpty(service.getGroup())) {
                localGroup.getChildren().add(service);
            } else {
                map.compute(service.getGroup(), (k, v) -> {
                    if (null == v) {
                        v = new ServiceInstance();
                        v.setNodeType(CommonConst.NODE_GROUP);
                        v.setSid(String.format("%08x", Objects.hash(SettingUtils.getUuid(), k)));
                        v.setName(service.getGroup());
                        v.setHost(selfHost);
                        v.setHostName(selfHostName);
                        v.setChildren(new ArrayList<>());
                        list.add(v);
                    }
                    v.getChildren().add(service);
                    return v;
                });
            }
        });
        localGroup.getChildren().addAll(list);
        return localGroup;
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

    public void addScheduleTask(ServiceSetting setting) {
        if (isScheduling(setting.getSid())) {
            throw new JarbootException("定时任务" + setting.getName() + "正在计划中");
        }
        if (!SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
            throw new JarbootException(setting.getName() + "非定时任务类型");
        }
        if (StringUtils.isEmpty(setting.getCron())) {
            throw new JarbootException("cron配置为空");
        }
        JobDetail job = JobBuilder.newJob(TaskJob.class)
                .usingJobData(CommonConst.USER_DIR, setting.getUserDir())
                .usingJobData(CommonConst.SERVICE_NAME_PARAM, setting.getName())
                .usingJobData(CommonConst.SID_PARAM, setting.getSid())
                .withIdentity(setting.getSid())
                .withDescription(setting.getName())
                .storeDurably()
                .build();
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(setting.getSid())
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(setting.getCron()))
                .build();
        try {
            scheduler.scheduleJob(job, trigger);
        } catch (Exception e) {
            throw new JarbootException(e);
        }
    }

    public void removeScheduleTask(ServiceSetting setting) {
        TriggerKey key = TriggerKey.triggerKey(setting.getSid());
        try {
            scheduler.pauseTrigger(key);
            scheduler.unscheduleJob(key);
            scheduler.deleteJob(JobKey.jobKey(setting.getSid()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void checkUserDir(File servicesDir) {
        if (servicesDir.isDirectory() || servicesDir.exists()) {
            return;
        }
        if (!servicesDir.mkdirs()) {
            throw new JarbootException(ResultCodeConst.INTERNAL_ERROR, servicesDir.getName() + "目录创建失败");
        }
    }

    private String getGroup(String userDir, String serviceName, String sid) {
        ServiceSetting setting = PropertyFileUtils.getServiceSettingBySid(sid);
        if (null != setting) {
            return setting.getGroup();
        }
        setting = PropertyFileUtils.getServiceSetting(userDir, serviceName);
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
        //订阅任务状态变化事件
        NotifyReactor.getInstance().registerSubscriber(new TaskStatusChangeSubscriber(this.eventRegistry), WebSocketMainServer.PUBLISHER);
    }
}
