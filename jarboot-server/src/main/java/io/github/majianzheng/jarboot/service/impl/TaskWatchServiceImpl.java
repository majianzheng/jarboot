package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.dao.UserDao;
import io.github.majianzheng.jarboot.service.TaskWatchService;
import io.github.majianzheng.jarboot.task.TaskRunCache;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.PropertyFileUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import io.github.majianzheng.jarboot.event.ServiceFileChangeEvent;
import io.github.majianzheng.jarboot.event.ServiceOfflineEvent;
import io.github.majianzheng.jarboot.event.ServiceOnlineEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
@Component
public class TaskWatchServiceImpl implements TaskWatchService, Subscriber<ServiceFileChangeEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Resource
    private TaskRunCache taskRunCache;
    @Resource
    private ServiceManagerImpl serverMgrService;
    @Resource
    private UserDao userDao;
    @Value("${jarboot.file-update-exclude:^[\\s\\S]*\\.(log[\\s\\S]*|pdf|png|jpeg|jpg|docx|doc|xls|xlsx|ppt|pjpg|md|txt|dat|ctrl|lck|lock)$}")
    private String fileUpdateExclude;
    private Pattern fileUpdatePattern;
    @Value("${jarboot.dir-update-exclude:(log|data|tmp|temp|cache|static)}")
    private String dirUpdateExclude;
    private Pattern dirUpdatePattern;
    private WatchService watchService;
    private final Map<WatchKey, ServiceSetting> watchKeyServiceMap = new ConcurrentHashMap<>(16);

    private final String jarbootHome = System.getProperty(CommonConst.JARBOOT_HOME);
    @Value("${jarboot.after-start-exec:}")
    private String afterStartExec;
    private boolean started = false;
    private final ThreadFactory threadFactory = JarbootThreadFactory
            .createThreadFactory("jarboot-tws", true);
    private final Thread monitorThread = threadFactory.newThread(this::initPathMonitor);

    /** 阻塞队列，监控到目录变化则放入队列 */
    private final LinkedBlockingQueue<ServiceSetting> modifiedServiceQueue = new LinkedBlockingQueue<>(1024);

    @Override
    public void init() {
        if (started) {
            return;
        }
        started = true;
        // 初始化
        fileUpdatePattern = Pattern.compile(fileUpdateExclude);
        dirUpdatePattern = Pattern.compile(dirUpdateExclude);
        // 路径监控生产者
        this.monitorThread.start();

        // 路径监控消费者
        NotifyReactor.getInstance().registerSubscriber(this);

        // attach已经处于启动的进程
        this.attachRunningServer();

        // 注册事件处理
        registerEventHandler();
        // 清理无效的记录文件
        cleanRecordFiles();

        // 是否自动启动服务
        if (Boolean.TRUE.equals(SettingUtils.getSystemSetting().getServicesAutoStart())) {
            threadFactory
                    .newThread(this::autoStartServices)
                    .start();
        }
        //启动后置脚本
        if (StringUtils.isNotEmpty(afterStartExec)) {
            final String bashFileExt = OSUtils.isWindows() ? "cmd" : "sh";
            final String bashFileName = String.format("after_start_exec.%s", bashFileExt);
            final File bashFile = FileUtils.getFile(CacheDirHelper.getTempBashDir(), bashFileName);
            threadFactory
                    .newThread(() -> TaskUtils.startTask(afterStartExec, null, jarbootHome, bashFile, SettingUtils.getJdkPath()))
                    .start();
        }
    }

    @Override
    public void registerServiceChangeMonitor(ServiceSetting setting) {
        if (!SettingPropConst.SCHEDULE_LONE.equals(setting.getScheduleType()) || !Boolean.TRUE.equals(setting.getFileUpdateWatch())) {
            return;
        }
        final Path servicePath = Paths.get(SettingUtils.getWorkspace(), setting.getUserDir(), setting.getName());
        //先遍历所有jar文件，将文件的最后修改时间记录下来
        storeCurFileModifyTime(setting);
        //给path路径加上文件观察服务
        try {
            WatchKey watchKey = servicePath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            watchKeyServiceMap.put(watchKey, setting);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            MessageUtils.error(String.format("注册服务（%s）文件变动监控失败！", setting.getName()));
        }
    }

    @Override
    public void unregisterServiceChangeMonitor(String sid) {
        WatchKey watchKey = null;
        for (Map.Entry<WatchKey, ServiceSetting> entry : watchKeyServiceMap.entrySet()) {
            if (Objects.equals(sid, entry.getValue().getSid())) {
                watchKey = entry.getKey();
                break;
            }
        }
        if (null != watchKey) {
            try {
                watchKey.cancel();
                File recordFile = getRecordFile(sid);
                FileUtils.deleteQuietly(recordFile);
                watchKeyServiceMap.remove(watchKey);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onEvent(ServiceFileChangeEvent event) {
        //取出后
        HashSet<ServiceSetting> services = new HashSet<>();
        try {
            ServiceSetting serviceSetting;
            //防抖去重，总是延迟一段时间（抖动时间配置），变化多次计一次
            while (null != (serviceSetting = modifiedServiceQueue.poll(SettingUtils.getSystemSetting().getFileChangeShakeTime(), TimeUnit.SECONDS))) {
                services.add(serviceSetting);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (services.isEmpty()) {
            return;
        }
        //过滤掉jar文件未变化掉服务，判定jar文件掉修改时间是否一致
        List<ServiceSetting> list = services.stream().filter(this::checkFileUpdate).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(list)) {
            final String msg = "监控到工作空间文件更新，开始重启相关服务...";
            MessageUtils.info(msg);
            TaskUtils.getTaskExecutor().execute(() -> list.forEach(setting -> {
                serverMgrService.stopSingleService(setting);
                serverMgrService.startSingleService(setting);
            }));
        }
    }

    @Override
    public Executor executor() {
        return TaskUtils.getTaskExecutor();
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return ServiceFileChangeEvent.class;
    }

    private void attachRunningServer() {
        List<String> sidList = PidFileHelper.getAllSid();
        if (CollectionUtils.isEmpty(sidList)) {
            return;
        }
        sidList.forEach(this::doAttachRunningServer);
    }

    /**
     * 启动目录变动监控
     */
    private void initPathMonitor() {
        //启动路径监控
        try {
            watchService = FileSystems.getDefault().newWatchService();
            //初始化路径监控 开始监控
            pathWatchMonitor();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            MessageUtils.error("工作空间监控异常：" + ex.getMessage());
        } finally {
            try {
                watchService.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 缓存文件的时间戳
     */
    private void storeCurFileModifyTime(ServiceSetting setting) {
        String servicePath = SettingUtils.getServicePath(setting.getUserDir(), setting.getName());
        Collection<File> files = filterUpdateFile(FileUtils.getFile(servicePath));
        if (!CollectionUtils.isEmpty(files)) {
            File recordFile = getRecordFile(setting.getSid());
            if (null != recordFile) {
                Properties properties = new Properties();
                files.forEach(jarFile -> properties.put(genFileHashKey(jarFile),
                        String.valueOf(jarFile.lastModified())));
                PropertyFileUtils.storeProperties(recordFile, properties);
            }
        }
    }

    private Collection<File> filterUpdateFile(File dir) {
        IOFileFilter filter = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return doFilterUpdateFile(file);
            }

            @Override
            public boolean accept(File dir, String name) {
                return doFilterUpdateFile(dir);
            }
        };
        return FileUtils.listFiles(dir, filter, filter);
    }

    private boolean doFilterUpdateFile(File file) {
        if (file.isDirectory()) {
            if (dirUpdatePattern.matcher(file.getName()).matches()) {
                return false;
            }
        } else {
            if (fileUpdatePattern.matcher(file.getName()).matches()) {
                return false;
            }
        }
        if (file.isHidden()) {
            return false;
        }
        if (!OSUtils.isWindows()) {
            return !file.getName().startsWith(".");
        }
        return true;
    }

    private File getRecordFile(String sid) {
        File recordFile = CacheDirHelper.getMonitorRecordFile(sid);
        if (!recordFile.exists()) {
            try {
                if (!recordFile.createNewFile()) {
                    logger.warn("createNewFile({}) failed.", recordFile.getPath());
                    return null;
                }
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
                return null;
            }
        }
        return recordFile;
    }

    private void pathWatchMonitor() {
        for (;;) {
            try {
                final WatchKey key = watchService.take();
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    handlePathEvent(key, watchEvent);
                }
                if (!key.reset()) {
                    logger.error("处理失败，重置错误");
                    MessageUtils.error("文件变更监控异常，请重启Jarboot解决！");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!started) {
                break;
            }
        }
    }

    private void handlePathEvent(WatchKey watchKey, WatchEvent<?> watchEvent) throws InterruptedException {
        ServiceSetting setting = watchKeyServiceMap.get(watchKey);
        final WatchEvent.Kind<?> kind = watchEvent.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW || null == setting) {
            return;
        }
        //创建事件或修改事件
        if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            //创建或修改文件
            if (!AgentManager.getInstance().isOnline(setting.getSid())) {
                //当前不处于正在运行的状态
                return;
            }
            if (Boolean.TRUE.equals(setting.getFileUpdateWatch())) {
                //启用了路径监控配置
                modifiedServiceQueue.put(setting);
                ServiceFileChangeEvent event = new ServiceFileChangeEvent();
                event.setSetting(setting);
                NotifyReactor.getInstance().publishEvent(event);
            }
        }
    }

    private String genFileHashKey(File jarFile) {
        String path = jarFile.getAbsolutePath();
        return String.format("hash.%08x", path.hashCode());
    }

    private boolean checkFileUpdate(ServiceSetting setting) {
        String serverDir = SettingUtils.getServicePath(setting.getUserDir(), setting.getName());
        Collection<File> files = filterUpdateFile(FileUtils.getFile(serverDir));
        if (CollectionUtils.isEmpty(files)) {
            return false;
        }
        File recordFile = getRecordFile(setting.getSid());
        if (null == recordFile) {
            return false;
        }
        Properties recordProps = PropertyFileUtils.getProperties(recordFile);
        boolean updateFlag = false;
        for (File file : files) {
            String key = genFileHashKey(file);
            String value = recordProps.getProperty(key, "-1");
            long lastModifyTime = Long.parseLong(value);
            if (lastModifyTime != file.lastModified()) {
                recordProps.setProperty(key, String.valueOf(file.lastModified()));
                updateFlag = true;
            }
        }
        if (updateFlag) {
            //更新jar文件掉最后修改时间
            PropertyFileUtils.storeProperties(recordFile, recordProps);
        }
        return updateFlag;
    }

    private void doAttachRunningServer(String sid) {
        if (AgentManager.getInstance().isOnline(sid)) {
            //已经是在线状态
            return;
        }
        TaskUtils.attach(sid);
    }

    private void registerEventHandler() {
        NotifyReactor.getInstance().registerSubscriber(new Subscriber<ServiceOnlineEvent>() {
            @Override
            public void onEvent(ServiceOnlineEvent event) {
                if (null == event.getSetting()) {
                    return;
                }
                registerServiceChangeMonitor(event.getSetting());
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return ServiceOnlineEvent.class;
            }
        });

        NotifyReactor.getInstance().registerSubscriber(new Subscriber<ServiceOfflineEvent>() {
            @Override
            public void onEvent(ServiceOfflineEvent event) {
                if (null == event.getSetting()) {
                    return;
                }
                unregisterServiceChangeMonitor(event.getSetting().getSid());
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return ServiceOfflineEvent.class;
            }
        });
    }

    private void autoStartServices() {
        File workspaceDir = FileUtils.getFile(SettingUtils.getWorkspace());
        File[] files = workspaceDir.listFiles();
        if (null == files) {
            return;
        }
        for (File userDir : files) {
            String name = userDir.getName();
            if (userDao.existsByUserDir(name)) {
                List<ServiceInstance> serviceList = taskRunCache.getServiceList(name);
                List<String> services = serviceList.stream().map(ServiceInstance::getName).collect(Collectors.toList());
                logger.info("开始自动启动服务目录{}, 服务数量：{}", name, services.size());
                serverMgrService.startService0(name, services);
                logger.info("自动启动服务目录{}完成！", name);
            } else {
                logger.warn("工作空间下，该目录（{}）没有任何用户关联", name);
            }
        }
    }

    private void cleanRecordFiles() {
        File recordDir = CacheDirHelper.getMonitorRecordDir();
        if (!recordDir.exists() && !recordDir.mkdirs()) {
            logger.error("创建record目录（{}）失败！", recordDir.getPath());
            return;
        }
        //清理失效的record文件
        File[] recordFiles = recordDir.listFiles();
        if (null != recordFiles) {
            for (File recordFile : recordFiles) {
                String sid = recordFile.getName().replace(".snapshot", StringUtils.EMPTY);
                if (StringUtils.isEmpty(TaskUtils.getPid(sid))) {
                    FileUtils.deleteQuietly(recordFile);
                }
            }
        }
    }
}
