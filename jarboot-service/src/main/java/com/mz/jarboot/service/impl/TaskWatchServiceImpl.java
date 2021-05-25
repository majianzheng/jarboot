package com.mz.jarboot.service.impl;

import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dao.TaskRunDao;
import com.mz.jarboot.dto.ServerSettingDTO;
import com.mz.jarboot.event.AgentOfflineEvent;
import com.mz.jarboot.event.TaskEvent;
import com.mz.jarboot.event.TaskEventEnum;
import com.mz.jarboot.service.TaskWatchService;
import com.mz.jarboot.utils.PropertyFileUtils;
import com.mz.jarboot.utils.SettingUtils;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

@Component
public class TaskWatchServiceImpl implements TaskWatchService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ApplicationContext ctx; //应用上下文
    @Autowired
    private ExecutorService taskExecutor;
    @Autowired
    private TaskRunDao taskRunDao;

    //阻塞队列，监控到目录变化则放入队列
    private final ArrayBlockingQueue<String> modifiedServiceQueue = new ArrayBlockingQueue<>(32);

    @PostConstruct
    public void init() {
        //路径监控生产者
        taskExecutor.execute(this::initPathMonitor);
        //路径监控消费者
        taskExecutor.execute(() -> {
            for (;;) {
                try {
                    String server = modifiedServiceQueue.take();
                    //取出后
                    Set<String> services = new HashSet<>();
                    services.add(server);
                    while (null != (server = modifiedServiceQueue.poll(5, TimeUnit.SECONDS))) {
                        services.add(server);
                    }
                    //防抖去重，总是延迟5秒钟，变化多次计一次
                    List<String> list = new ArrayList<>(services);
                    TaskEvent event = new TaskEvent();
                    event.setEventType(TaskEventEnum.RESTART);
                    event.setServices(list);
                    ctx.publishEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void initPathMonitor() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            //初始化路径监控
            String servicesPath = SettingUtils.getServicesPath();
            final Path monitorPath = Paths.get(servicesPath);
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
            //创建或修改文件
            if (!CommonConst.STATUS_RUNNING.equals(taskRunDao.getTaskStatus(service))) {
                //当前不处于正在运行的状态
                return;
            }
            ServerSettingDTO setting = PropertyFileUtils.getServerSetting(service);
            if (Boolean.TRUE.equals(setting.getJarUpdateWatch())) {
                //启用了路径监控配置
                modifiedServiceQueue.put(service);
            }
        }
        //删除事件
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            logger.info("触发了删除事件，忽略执行，文件：{}", watchEvent.context());
        }
    }

    /**
     * 是否启用路径监控
     *
     * @param enabled 是否启用
     */
    @Override
    public void enablePathWatch(Boolean enabled) {
        throw new UnsupportedOperationException();
    }

    /**
     * 添加要守护的服务
     *
     * @param serviceName 服务名
     */
    @Override
    public void addDaemonService(String serviceName) {
        throw new UnsupportedOperationException();
    }

    /**
     * 移除要守护的服务
     *
     * @param serviceName 服务名
     */
    @Override
    public void removeDaemonService(String serviceName) {
        throw new UnsupportedOperationException();
    }

    @EventListener
    public void onAgentOfflineEvent(AgentOfflineEvent event) {
        String server = event.getServer();
        //检查进程是否存活
        if (TaskUtils.isAlive(server)) {
            //检查是否处于中间状态
            String status = taskRunDao.getTaskStatus(server);
            if (CommonConst.STATUS_STARTING.equals(status) || CommonConst.STATUS_STOPPING.equals(status)) {
                //处于中间状态，此时不做干预，守护只针对正在运行的进程
                return;
            }
        }

        //获取是否开启了守护
        ServerSettingDTO setting = PropertyFileUtils.getServerSetting(server);
        final SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
        String s = sdf.format(new Date(event.getOfflineTime()));
        if (Boolean.TRUE.equals(setting.getDaemon())) {
            WebSocketManager.getInstance().noticeWarn(String.format("服务%s于%s异常退出，即将启动守护启动！", server, s));
            List<String> list = new ArrayList<>();
            list.add(server);
            TaskEvent ev = new TaskEvent();
            ev.setEventType(TaskEventEnum.DAEMON_START);
            ev.setServices(list);
            ctx.publishEvent(ev);
        } else {
            WebSocketManager.getInstance().noticeWarn(String.format("服务%s于%s异常退出，请检查服务状态！", server, s));
        }
    }
}
