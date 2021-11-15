package com.mz.jarboot.service.impl;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.task.TaskRunCache;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.event.TaskEvent;
import com.mz.jarboot.event.TaskEventEnum;
import com.mz.jarboot.service.TaskWatchService;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author majianzheng
 */
@Component
public class TaskWatchServiceImpl implements TaskWatchService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String MODIFY_TIME_STORE_FILE = "file-record.temp";
    private static final int MIN_MODIFY_WAIT_TIME = 3;
    private static final int MAX_MODIFY_WAIT_TIME = 600;

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private TaskRunCache taskRunCache;
    private final String jarbootHome = System.getProperty(CommonConst.JARBOOT_HOME);

    @Value("${jarboot.file-shake-time:5}")
    private long modifyWaitTime;
    @Value("${jarboot.after-start-exec:}")
    private String afterStartExec;
    @Value("${jarboot.services.enable-auto-start-after-start:false}")
    private boolean enableAutoStartServices;
    private String curWorkspace;
    private boolean starting = false;

    /** 阻塞队列，监控到目录变化则放入队列 */
    private final ArrayBlockingQueue<String> modifiedServiceQueue = new ArrayBlockingQueue<>(32);

    @Override
    public void init() {
        if (starting) {
            return;
        }
        if (modifyWaitTime < MIN_MODIFY_WAIT_TIME || modifyWaitTime > MAX_MODIFY_WAIT_TIME) {
            modifyWaitTime = 5;
        }
        starting = true;
        ExecutorService taskExecutor = TaskUtils.getTaskExecutor();
        //路径监控生产者
        taskExecutor.execute(this::initPathMonitor);
        //路径监控消费者
        taskExecutor.execute(this::pathMonitorConsumer);

        //attach已经处于启动的进程
        taskExecutor.execute(this::attachRunningServer);

        if (enableAutoStartServices) {
            logger.info("Auto starting services...");
            TaskEvent ev = new TaskEvent(TaskEventEnum.AUTO_START_ALL);
            ctx.publishEvent(ev);
        }

        //启动后置脚本
        if (StringUtils.isNotEmpty(afterStartExec)) {
            taskExecutor.execute(() -> TaskUtils.startTask(afterStartExec, null, jarbootHome));
        }
    }

    private void attachRunningServer() {
        List<ServerRunning> runningServers = taskRunCache.getServerList();
        if (CollectionUtils.isEmpty(runningServers)) {
            return;
        }
        runningServers.forEach(this::doAttachRunningServer);
    }

    /**
     * 启动目录变动监控
     */
    private void initPathMonitor() {
        //先遍历所有jar文件，将文件的最后修改时间记录下来
        storeCurFileModifyTime();
        //启动路径监控
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            //初始化路径监控
            curWorkspace = SettingUtils.getWorkspace();
            final Path monitorPath = Paths.get(curWorkspace);
            //给path路径加上文件观察服务
            monitorPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            //开始监控
            pathWatchMonitor(watchService);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 目录监控事件处理
     */
    private void pathMonitorConsumer() {
        try {
            for (; ; ) {
                String path = modifiedServiceQueue.take();
                //取出后
                HashSet<String> services = new HashSet<>();
                services.add(path);
                //防抖去重，总是延迟一段时间（抖动时间配置），变化多次计一次
                while (null != (path = modifiedServiceQueue.poll(modifyWaitTime, TimeUnit.SECONDS))) {
                    services.add(path);
                }
                //过滤掉jar文件未变化掉服务，判定jar文件掉修改时间是否一致
                List<String> list = services.stream().filter(this::checkJarUpdate).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(list)) {
                    TaskEvent event = new TaskEvent(TaskEventEnum.RESTART);
                    event.setPaths(list);
                    logger.debug("服务文件变动，启动重启！{}", list);
                    final String msg = "文件更新，开始重启...";
                    WebSocketManager.getInstance().notice(msg, NoticeEnum.INFO);
                    ctx.publishEvent(event);
                }
                if (!starting) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void storeCurFileModifyTime() {
        File[] serverDirs = taskRunCache.getServerDirs();
        if (null == serverDirs || serverDirs.length <= 0) {
            return;
        }
        for (File serverDir : serverDirs) {
            Collection<File> files = FileUtils.listFiles(serverDir, CommonConst.JAR_FILE_EXT, true);
            if (CollectionUtils.isNotEmpty(files)) {
                File recordFile = getRecordFile(serverDir);
                if (null != recordFile) {
                    Properties properties = new Properties();
                    files.forEach(jarFile -> properties.put(genFileHashKey(jarFile),
                            String.valueOf(jarFile.lastModified())));
                    PropertyFileUtils.storeProperties(recordFile, properties);
                }
            }
        }
    }

    private File getRecordFile(File serverDir) {
        File recordFile = new File(serverDir, MODIFY_TIME_STORE_FILE);
        if (!recordFile.exists()) {
            try {
                if (!recordFile.createNewFile()) {
                    logger.warn("createNewFile({}) failed.", recordFile.getPath());
                }
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
                return null;
            }
        }
        return recordFile;
    }

    private void pathWatchMonitor(WatchService watchService) throws InterruptedException {
        for (;;) {
            final WatchKey key = watchService.take();
            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                handlePathEvent(watchEvent);
            }
            if (!key.reset()) {
                logger.error("处理失败，重置错误");
                break;
            }
        }
    }

    private void handlePathEvent(WatchEvent<?> watchEvent) throws InterruptedException {
        final WatchEvent.Kind<?> kind = watchEvent.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW) {
            return;
        }
        String service = watchEvent.context().toString();
        //创建事件或修改事件
        if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            String path = curWorkspace + File.separator + service;
            String sid= SettingUtils.createSid(path);
            //创建或修改文件
            if (!AgentManager.getInstance().isOnline(sid)) {
                //当前不处于正在运行的状态
                return;
            }
            String serverPath = String.format("%s%s%s", curWorkspace, File.separator, service);
            ServerSetting setting = PropertyFileUtils.getServerSetting(serverPath);
            if (Boolean.TRUE.equals(setting.getJarUpdateWatch()) && StringUtils.equals(sid, setting.getSid())) {
                //启用了路径监控配置
                modifiedServiceQueue.put(path);
            }
        }
        //删除事件
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            logger.info("触发了删除事件，忽略执行，文件：{}", watchEvent.context());
        }
    }

    private String genFileHashKey(File jarFile) {
        String path = jarFile.getPath();
        return String.format("hash.%d", path.hashCode());
    }
    private boolean checkJarUpdate(String path) {
        File serverDir = new File(path);
        Collection<File> files = FileUtils.listFiles(serverDir, CommonConst.JAR_FILE_EXT, true);
        if (CollectionUtils.isEmpty(files)) {
            return false;
        }
        File recordFile = getRecordFile(serverDir);
        Properties recordProps = PropertyFileUtils.getProperties(recordFile);
        boolean updateFlag = false;
        for (File file : files) {
            String key = genFileHashKey(file);
            Object value = recordProps.get(key);
            long lastModifyTime = -1L;
            if (value instanceof String) {
                lastModifyTime = NumberUtils.toLong((String)value, -1L);
            }
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

    private void doAttachRunningServer(ServerRunning server) {
        if (AgentManager.getInstance().isOnline(server.getSid())) {
            //已经是在线状态
            return;
        }
        TaskUtils.attach(server.getName(), server.getSid());
    }
}
