package io.github.majianzheng.jarboot.service.impl;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import io.github.majianzheng.jarboot.api.constant.TaskLifecycle;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.JvmProcess;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.notify.AbstractEventRegistry;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.VMUtils;
import io.github.majianzheng.jarboot.task.AttachStatus;
import io.github.majianzheng.jarboot.task.TaskRunCache;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.event.ServiceOfflineEvent;
import io.github.majianzheng.jarboot.utils.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * 服务管理
 * @author majianzheng
 */
@Service
public class ServiceManagerImpl implements ServiceManager, Subscriber<ServiceOfflineEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String STARTED_MSG = "\033[96;1m%s\033[0m started cost \033[91;1m%.3f\033[0m second.\033[5m✨\033[0m";
    private static final String STOPPED_MSG = "\033[96;1m%s\033[0m stopped cost \033[91;1m%.3f\033[0m second.";

    @Autowired
    private TaskRunCache taskRunCache;
    @Autowired
    private AbstractEventRegistry eventRegistry;
    @Resource(name = "taskExecutorService")
    private ExecutorService executorService;

    @Override
    public List<ServiceInstance> getServiceList() {
        return taskRunCache.getServiceList(SettingUtils.getCurrentUserDir());
    }

    @Override
    public ServiceInstance getServiceGroup() {
        return taskRunCache.getServiceGroup(SettingUtils.getCurrentUserDir());
    }

    @Override
    public JvmProcess getJvmGroup() {
        JvmProcess localGroup = new JvmProcess();
        localGroup.setNodeType(CommonConst.NODE_ROOT);
        localGroup.setSid(String.format("%08x", SettingUtils.getUuid().hashCode()));
        localGroup.setHost(ClusterClientManager.getInstance().getSelfHost());
        localGroup.setHostName(ClusterClientManager.getInstance().getSelfHostName());
        localGroup.setChildren(new ArrayList<>());
        localGroup.getChildren().addAll(this.getJvmProcesses());
        return localGroup;
    }

    /**
     * 获取服务信息
     *
     * @param serviceName 服务名称
     * @return 服务信息 {@link ServiceInstance}
     */
    @Override
    public ServiceInstance getService(String serviceName) {
        String userDir = SettingUtils.getCurrentUserDir();
        return taskRunCache.getService(SettingUtils.getCurrentUserDir(), FileUtils.getFile(SettingUtils.getWorkspace(), userDir, serviceName));
    }

    /**
     * 启动服务
     *
     * @param serviceNames 服务列表，字符串格式：服务path
     */
    @Override
    public void startService(List<String> serviceNames) {
        if (CollectionUtils.isEmpty(serviceNames)) {
            return;
        }
        String userDir = SettingUtils.getCurrentUserDir();
        //在线程池中执行，防止前端请求阻塞超时
        TaskUtils.getTaskExecutor().execute(() -> this.startService0(userDir, serviceNames));
    }

    void startService0(String userDir, List<String> services) {
        //获取服务的优先级启动顺序
        final Queue<ServiceSetting> priorityQueue = PropertyFileUtils.parseStartPriority(userDir, services);
        ArrayList<ServiceSetting> taskList = new ArrayList<>();
        ServiceSetting setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting);
            ServiceSetting next = priorityQueue.peek();
            if (null != next && !next.getPriority().equals(setting.getPriority())) {
                //同一级别的全部取出
                startServiceGroup(taskList);
                //开始指定下一级的启动组，此时上一级的已经全部启动完成，清空组
                taskList.clear();
            }
        }
        //最后一组的启动
        startServiceGroup(taskList);
    }

    /**
     * 同一级别的一起启动
     * @param s 同级服务列表
     */
    private void startServiceGroup(List<ServiceSetting> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(setting ->
                executorService.execute(() -> {
                    try {
                        this.startSingleService(setting);
                    } finally {
                        countDownLatch.countDown();
                    }
                }));

        try {
            //等待全部启动完成
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 根据服务配置，启动单个服务
     * @param setting 服务配置
     */
    @Override
    public void startSingleService(ServiceSetting setting) {
        String server = setting.getName();
        String sid = setting.getSid();
        // 已经处于启动中或停止中时不允许执行开始，但是开始中时应当可以执行停止，用于异常情况下强制停止
        if (this.taskRunCache.isStopping(sid)) {
            MessageUtils.info("服务" + server + "正在停止");
            return;
        }
        if (AgentManager.getInstance().isOnline(sid)) {
            //已经启动
            MessageUtils.upgradeStatus(sid, CommonConst.RUNNING);
            MessageUtils.info("服务" + server + "已经是启动状态");
            return;
        }
        if (!this.taskRunCache.addStarting(sid)) {
            MessageUtils.info("服务" + server + "正在启动中");
            return;
        }
        try {
            //设定启动中，并发送前端让其转圈圈
            NotifyReactor
                    .getInstance()
                    .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.PRE_START));
            //记录开始时间
            long startTime = System.currentTimeMillis();
            //开始启动进程
            if (SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
                taskRunCache.addScheduleTask(setting);
            } else {
                TaskUtils.startService(setting);
            }
            //记录启动结束时间，减去判定时间修正

            double costTime = (System.currentTimeMillis() - startTime)/1000.0f;
            //服务是否启动成功
            if (AgentManager.getInstance().isOnline(sid)) {
                MessageUtils.console(sid, String.format(STARTED_MSG, server, costTime));
                NotifyReactor
                        .getInstance()
                        .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.AFTER_STARTED));
            } else {
                //启动失败
                if (SettingPropConst.SCHEDULE_LONE.equals(setting.getScheduleType())) {
                    NotifyReactor
                            .getInstance()
                            .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.START_FAILED));
                    MessageUtils.error("启动服务" + server + "失败！");
                } else if (SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
                    NotifyReactor
                            .getInstance()
                            .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.SCHEDULING));
                    MessageUtils.info("服务" + server + "已加入定时任务计划");
                } else {
                    MessageUtils.console(sid, String.format(STARTED_MSG, server, costTime));
                    NotifyReactor
                            .getInstance()
                            .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.FINISHED));
                    MessageUtils.info("启动服务" + server + "完成！");
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            MessageUtils.printException(sid, e);
            NotifyReactor
                    .getInstance()
                    .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.START_FAILED));
            MessageUtils.error("启动服务" + server + "失败！");
        } finally {
            this.taskRunCache.removeStarting(sid);
            TaskUtils.cleanBashFile(SettingUtils.getServicePath(setting.getUserDir(), setting.getName()));
        }
    }

    /**
     * 停止服务
     *
     * @param serviceNames 服务列表，字符串格式：服务path
     */
    @Override
    public void stopService(List<String> serviceNames) {
        if (CollectionUtils.isEmpty(serviceNames)) {
            return;
        }
        String userDir = SettingUtils.getCurrentUserDir();
        //在线程池中执行，防止前端请求阻塞超时
        TaskUtils.getTaskExecutor().execute(() -> this.stopService0(userDir, serviceNames));
    }

    @Override
    public List<JvmProcess> getJvmProcesses() {
        ArrayList<JvmProcess> result = new ArrayList<>();
        Map<String, String> vms = VMUtils.getInstance().listVM();
        vms.forEach((pid, v) -> {
            if (AgentManager.getInstance().isLocalService(pid)) {
                return;
            }
            JvmProcess process = new JvmProcess();
            String sid = CommonUtils.createJvmSid(pid);
            process.setHost(ClusterClientManager.getInstance().getSelfHost());
            process.setHostName(ClusterClientManager.getInstance().getSelfHostName());
            process.setSid(sid);
            process.setPid(pid);
            String status = AgentManager.getInstance().isOnline(sid) ? CommonConst.ATTACHED : CommonConst.NOT_ATTACHED;
            process.setStatus(status);
            process.setFullName(v);
            //解析获取简略名字
            process.setName(TaskUtils.parseCommandSimple(v));
            result.add(process);
        });
        Collection<JvmProcess> remoteGroups = getRemoteJvmGroup();
        if (!remoteGroups.isEmpty()) {
            result.addAll(remoteGroups);
        }
        return result;
    }

    @Override
    public void attach(String pid) {
        if (StringUtils.isEmpty(pid)) {
            throw new JarbootException("pid is empty!");
        }
        String sid = CommonUtils.createJvmSid(pid);
        Object vm = null;
        MessageUtils.upgradeStatus(sid, AttachStatus.ATTACHING);
        try {
            vm = VMUtils.getInstance().attachVM(pid);
            String args = SettingUtils.getLocalhost();
            VMUtils.getInstance().loadAgentToVM(vm, SettingUtils.getAgentJar(), args);
        } catch (Exception e) {
            MessageUtils.printException(sid, e);
        } finally {
            if (null != vm) {
                VMUtils.getInstance().detachVM(vm);
            }
        }
    }

    @Override
    public void deleteService(String serviceName) {
        String path = SettingUtils.getServicePath(serviceName);
        String sid = SettingUtils.createSid(path);
        if (this.taskRunCache.isStartingOrStopping(sid)) {
            throw new JarbootRunException(serviceName + "在停止中或启动中，不可删除！");
        }
        if (AgentManager.getInstance().isOnline(sid)) {
            throw new JarbootRunException(serviceName + "正在运行，不可删除！");
        }
        MessageUtils.globalLoading(serviceName, serviceName + "删除中...");
        executorService.execute(() -> {
            try {
                FileUtils.deleteDirectory(FileUtils.getFile(path));
                MessageUtils.globalEvent(FrontEndNotifyEventType.WORKSPACE_CHANGE);
                MessageUtils.info("删除" + serviceName + "成功！");
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                MessageUtils.error("删除" + serviceName + "失败！" + e.getMessage());
            } finally {
                MessageUtils.globalLoading(serviceName, StringUtils.EMPTY);
            }
        });
    }

    /**
     * 注册事件处理
     *
     * @param serviceName 服务名称
     * @param lifecycle   任务生命周期 {@link TaskLifecycle}
     * @param subscriber  任务处理 {@link Subscriber}
     */
    @Override
    public void registerSubscriber(String serviceName,
                                   TaskLifecycle lifecycle,
                                   Subscriber<TaskLifecycleEvent> subscriber) {
        final String topic = eventRegistry.createTopic(TaskLifecycleEvent.class, serviceName, lifecycle.name());
        eventRegistry.registerSubscriber(topic, subscriber);
    }

    /**
     * 反注册事件处理
     *
     * @param serviceName 服务名称
     * @param lifecycle   任务生命周期 {@link TaskLifecycle}
     * @param subscriber  任务处理 {@link Subscriber}
     */
    @Override
    public void deregisterSubscriber(String serviceName,
                                     TaskLifecycle lifecycle,
                                     Subscriber<TaskLifecycleEvent> subscriber) {
        final String topic = eventRegistry.createTopic(TaskLifecycleEvent.class, serviceName, lifecycle.name());
        eventRegistry.deregisterSubscriber(topic, subscriber);
    }

    private void stopService0(String userDir, List<String> paths) {
        //获取服务的优先级顺序，与启动相反的顺序依次终止
        final Queue<ServiceSetting> priorityQueue = PropertyFileUtils.parseStopPriority(userDir, paths);
        ArrayList<ServiceSetting> taskList = new ArrayList<>();
        ServiceSetting setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting);
            ServiceSetting next = priorityQueue.peek();
            if (null != next && !next.getPriority().equals(setting.getPriority())) {
                //同一级别的全部取出
                stopServiceGroup(taskList);
                //开始指定下一级的启动组，此时上一级的已经全部启动完成，清空组
                taskList.clear();
            }
        }
        //最后一组的启动
        stopServiceGroup(taskList);
    }

    private void stopServiceGroup(List<ServiceSetting> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(server ->
                executorService.execute(() -> {
                    try {
                        this.stopSingleService(server);
                    } finally {
                        countDownLatch.countDown();
                    }
                }));

        try {
            //等待全部终止完成
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stopSingleService(ServiceSetting setting) {
        String server = setting.getName();
        String sid = setting.getSid();
        if (!this.taskRunCache.addStopping(sid)) {
            MessageUtils.info("服务" + server + "正在停止中");
            return;
        }
        try {
            //发送停止中消息
            NotifyReactor
                    .getInstance()
                    .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.PRE_STOP));
            //记录开始时间
            long startTime = System.currentTimeMillis();
            TaskUtils.killService(sid);
            if (SettingPropConst.SCHEDULE_CRON.equals(setting.getScheduleType())) {
                taskRunCache.removeScheduleTask(setting);
                MessageUtils.info("服务" + server + "已移除定时任务计划");
            }
            //耗时
            double costTime = (System.currentTimeMillis() - startTime)/1000.0f;
            //停止成功
            if (AgentManager.getInstance().isOnline(sid)) {
                NotifyReactor
                        .getInstance()
                        .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.STOP_FAILED));
                MessageUtils.error("停止服务" + server + "失败！");
            } else {
                MessageUtils.console(sid, String.format(STOPPED_MSG, server, costTime));
                NotifyReactor
                        .getInstance()
                        .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.AFTER_STOPPED));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            MessageUtils.error(e.getMessage());
            MessageUtils.printException(sid, e);
        } finally {
            this.taskRunCache.removeStopping(sid);
            this.taskRunCache.removeStarting(sid);
            TaskUtils.cleanBashFile(SettingUtils.getServicePath(setting.getUserDir(), setting.getName()));
        }
    }

    /**
     * 重启服务
     *
     * @param serviceNames 服务列表，字符串格式：服务path
     */
    @Override
    public void restartService(List<String> serviceNames) {
        String userDir = SettingUtils.getCurrentUserDir();
        //获取终止的顺序
        TaskUtils.getTaskExecutor().execute(() -> {
            //先依次终止
            stopService0(userDir, serviceNames);
            //再依次启动
            startService0(userDir, serviceNames);
        });
    }

    @Override
    public void onEvent(ServiceOfflineEvent event) {
        ServiceSetting setting = event.getSetting();
        if (null == setting) {
            logger.debug("service offline event, service setting is null!");
            return;
        }
        String sid = setting.getSid();
        //检查是否处于中间状态
        if (event.isStopping() || taskRunCache.isStopping(sid)) {
            //处于停止中状态，此时不做干预，守护只针对正在运行的进程
            return;
        }
        //检查进程是否存活
        String pid = TaskUtils.getPid(sid);
        if (!pid.isEmpty()) {
            //尝试重新初始化代理客户端
            TaskUtils.getTaskExecutor().schedule(() -> tryReAttach(setting), 5, TimeUnit.SECONDS);
            return;
        }
        postHandleOfflineEvent(setting);
    }

    private void postHandleOfflineEvent(ServiceSetting setting) {
        String serviceName = setting.getName();
        if (taskRunCache.isStarting(setting.getSid())) {
            MessageUtils.warn(String.format("服务%s启动失败！", serviceName));
            return;
        }
        TaskLifecycleEvent lifecycleEvent = new TaskLifecycleEvent(setting, TaskLifecycle.EXCEPTION_OFFLINE);

        NotifyReactor.getInstance().publishEvent(lifecycleEvent);
        boolean temp = false;
        if (SettingPropConst.SCHEDULE_LONE.equals(setting.getScheduleType())) {
            if (Boolean.TRUE.equals(setting.getDaemon())) {
                temp = true;
            } else {
                MessageUtils.warn(String.format("服务%s于%s异常退出，请检查服务状态！", serviceName, currentTimeFormat()));
            }
        }
        final boolean daemon = temp;
        executorService.execute(() -> {
            if (StringUtils.isNotEmpty(SettingUtils.getSystemSetting().getAfterServerOfflineExec())) {
                TaskUtils.execServiceOfflineShell(setting);
            }
            //启动
            if (daemon) {
                MessageUtils.warn(String.format("服务%s于%s异常退出，即将启动守护启动！", serviceName, currentTimeFormat()));
                this.startSingleService(setting);
            }
        });
    }

    private static String currentTimeFormat() {
        final SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
        return sdf.format(new Date());
    }

    private void tryReAttach(ServiceSetting setting) {
        final String sid = setting.getSid();
        if (taskRunCache.isStartingOrStopping(sid) || AgentManager.getInstance().isOnline(sid) || taskRunCache.isScheduling(sid)) {
            return;
        }
        String pid = TaskUtils.getPid(sid);
        if (pid.isEmpty()) {
            postHandleOfflineEvent(setting);
            return;
        }
        logger.info("尝试重连服务，attach sid: {}", sid);
        TaskUtils.attach(sid);
    }

    @PostConstruct
    public void init() {
        NotifyReactor.getInstance().registerSubscriber(this);
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return ServiceOfflineEvent.class;
    }

    private static Collection<JvmProcess> getRemoteJvmGroup() {
        List<JvmProcess> remoteJvm = AgentManager.getInstance().remoteProcess();
        Map<String, JvmProcess> remoteGroupMap = new HashMap<>(16);
        for (JvmProcess jvm : remoteJvm) {
            remoteGroupMap.compute(jvm.getRemote(), (k, v) -> {
                if (null == v) {
                    v = new JvmProcess();
                    v.setName(k);
                    v.setHost(v.getHost());
                    v.setSid(CommonUtils.createJvmSid("jvm-group" + k));
                    v.setNodeType(CommonConst.NODE_GROUP);
                    v.setChildren(new ArrayList<>());
                }
                v.getChildren().add(jvm);
                return v;
            });
        }
        return remoteGroupMap.values();
    }
}
