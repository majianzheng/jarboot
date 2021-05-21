package com.mz.jarboot.service.impl;

import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dao.TaskRunDao;
import com.mz.jarboot.event.ContextEventPub;
import com.mz.jarboot.event.TaskEvent;
import com.mz.jarboot.event.TaskEventEnum;
import com.mz.jarboot.service.TaskWatchService;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
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
    @Value("${root-path:}")
    private String rootPath;
    //阻塞队列，监控到目录变化则放入队列
    private final ArrayBlockingQueue<String> modifiedServiceQueue = new ArrayBlockingQueue<>(32);

    @PostConstruct
    public void init() {
        ContextEventPub.getInstance().setContext(this.ctx);
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

        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        list.forEach(vmd -> {
            logger.info(vmd.displayName());
            if (vmd.displayName().contains("print-web")) {
                try {

                    VirtualMachine vm = VirtualMachine.attach(vmd.id());
                    vm.loadAgent("E:\\opensource\\jarboot\\jarboot-agent\\target\\jarboot-agent.jar", "127.0.0.1:9899");
                    logger.info("》》》attached 成功！id:{}", vmd.id());
                    vm.detach();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initPathMonitor() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            //初始化路径监控
            String servicesPath = this.rootPath + File.separator + CommonConst.SERVICES_DIR;
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
            if (CommonConst.STATUS_RUNNING.equals(taskRunDao.getTaskStatus(service))) {
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

    }

    /**
     * 添加要守护的服务
     *
     * @param serviceName 服务名
     */
    @Override
    public void addDaemonService(String serviceName) {

    }

    /**
     * 移除要守护的服务
     *
     * @param serviceName 服务名
     */
    @Override
    public void removeDaemonService(String serviceName) {

    }
}
