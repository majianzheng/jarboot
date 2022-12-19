package com.mz.jarboot.service.impl;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.TaskLifecycle;
import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.event.TaskLifecycleEvent;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServiceGroup;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.notify.AbstractEventRegistry;
import com.mz.jarboot.common.notify.FrontEndNotifyEventType;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.common.utils.VMUtils;
import com.mz.jarboot.event.*;
import com.mz.jarboot.task.AttachStatus;
import com.mz.jarboot.task.TaskRunCache;
import com.mz.jarboot.api.service.ServiceManager;
import com.mz.jarboot.utils.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
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

    @Value("${jarboot.after-server-error-offline:}")
    private String afterServerErrorOffline;

    @Autowired
    private TaskRunCache taskRunCache;
    @Autowired
    private AbstractEventRegistry eventRegistry;

    @Override
    public List<ServiceInstance> getServiceList() {
        return taskRunCache.getServiceList();
    }

    @Override
    public List<ServiceGroup> getServiceGroup() {
        List<ServiceInstance> serviceList = taskRunCache.getServiceList();
        List<ServiceGroup> groups = new ArrayList<>();
        if (CollectionUtils.isEmpty(serviceList)) {
            return groups;
        }
        HashMap<String, ServiceGroup> map = new HashMap<>(16);
        serviceList.forEach(service -> {
            map.compute(service.getGroup(), (k, v) -> {
                if (null == v) {
                    v = new ServiceGroup();
                    v.setName(service.getGroup());
                    v.setChildren(new ArrayList<>());
                    groups.add(v);
                }
                v.getChildren().add(service);
                return v;
            });
        });
        return groups;
    }

    /**
     * 获取服务信息
     *
     * @param serviceName 服务名称
     * @return 服务信息 {@link ServiceInstance}
     */
    @Override
    public ServiceInstance getService(String serviceName) {
        return taskRunCache.getService(FileUtils.getFile(SettingUtils.getWorkspace(), serviceName));
    }

    /**
     * 一键重启，杀死所有服务进程，根据依赖重启
     */
    @Override
    public void oneClickRestart() {
        if (this.taskRunCache.hasStartingOrStopping()) {
            // 有任务在中间态，不允许执行
            MessageUtils.info("存在未完成的任务，请稍后重启");
            return;
        }
        //获取所有的服务
        List<String> services = taskRunCache.getServiceNameList();
        //同步控制，保证所有的都杀死后再重启
        if (!CollectionUtils.isEmpty(services)) {
            //启动服务
            this.restartService(services);
        }
    }

    /**
     * 一键启动，根据依赖重启
     */
    @Override
    public void oneClickStart() {
        if (this.taskRunCache.hasStartingOrStopping()) {
            // 有任务在中间态，不允许执行
            MessageUtils.info("存在未完成的任务，请稍后启动");
            return;
        }
        List<String> services = taskRunCache.getServiceNameList();
        //启动服务
        this.startService(services);
    }

    /**
     * 一键停止，杀死所有服务进程
     */
    @Override
    public void oneClickStop() {
        if (this.taskRunCache.hasStartingOrStopping()) {
            // 有任务在中间态，不允许执行
            MessageUtils.info("存在未完成的任务，请稍后停止");
            return;
        }
        List<String> services = taskRunCache.getServiceNameList();
        //启动服务
        this.stopService(services);
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

        //在线程池中执行，防止前端请求阻塞超时
        TaskUtils.getTaskExecutor().execute(() -> this.startService0(serviceNames));
    }

    private void startService0(List<String> services) {
        //获取服务的优先级启动顺序
        final Queue<ServiceSetting> priorityQueue = PropertyFileUtils.parseStartPriority(services);
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
                TaskUtils.getTaskExecutor().execute(() -> {
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
            TaskUtils.startService(setting);
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
                NotifyReactor
                        .getInstance()
                        .publishEvent(new TaskLifecycleEvent(setting, TaskLifecycle.START_FAILED));
                MessageUtils.error("启动服务" + server + "失败！");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            MessageUtils.error(e.getMessage());
            MessageUtils.printException(sid, e);
        } finally {
            this.taskRunCache.removeStarting(sid);
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

        //在线程池中执行，防止前端请求阻塞超时
        TaskUtils.getTaskExecutor().execute(() -> this.stopService0(serviceNames));
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
            process.setSid(pid);
            process.setPid(pid);
            process.setAttached(AgentManager.getInstance().isOnline(pid));
            process.setFullName(v);
            //解析获取简略名字
            process.setName(TaskUtils.parseCommandSimple(v));
            result.add(process);
        });
        AgentManager.getInstance().remoteProcess(result);
        return result;
    }

    @Override
    public void attach(String pid) {
        if (StringUtils.isEmpty(pid)) {
            throw new JarbootException("pid is empty!");
        }
        Object vm = null;
        MessageUtils.upgradeStatus(pid, AttachStatus.ATTACHING);
        try {
            vm = VMUtils.getInstance().attachVM(pid);
            String args = SettingUtils.getLocalhost();
            VMUtils.getInstance().loadAgentToVM(vm, SettingUtils.getAgentJar(), args);
        } catch (Exception e) {
            MessageUtils.printException(pid, e);
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
        TaskUtils.getTaskExecutor().execute(() -> {
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

    private void stopService0(List<String> paths) {
        //获取服务的优先级顺序，与启动相反的顺序依次终止
        final Queue<ServiceSetting> priorityQueue = PropertyFileUtils.parseStopPriority(paths);
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
                TaskUtils.getTaskExecutor().execute(() -> {
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

    private void stopSingleService(ServiceSetting setting) {
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
        }
    }

    /**
     * 重启服务
     *
     * @param serviceNames 服务列表，字符串格式：服务path
     */
    @Override
    public void restartService(List<String> serviceNames) {
        //获取终止的顺序
        TaskUtils.getTaskExecutor().execute(() -> {
            //先依次终止
            stopService0(serviceNames);
            //再依次启动
            startService0(serviceNames);
        });
    }

    @Override
    public void onEvent(ServiceOfflineEvent event) {
        String serviceName = event.getServiceName();
        String sid = event.getSid();
        //检查进程是否存活
        String pid = TaskUtils.getPid(sid);
        if (!pid.isEmpty()) {
            //检查是否处于中间状态
            if (taskRunCache.isStopping(sid)) {
                //处于停止中状态，此时不做干预，守护只针对正在运行的进程
                return;
            }
            //尝试重新初始化代理客户端
            TaskUtils.attach(sid);
            return;
        }
        //获取是否开启了守护
        ServiceSetting temp = PropertyFileUtils.getServiceSettingBySid(sid);
        //检测配置更新
        final ServiceSetting setting = null == temp ? null : PropertyFileUtils.getServiceSetting(temp.getName());

        TaskLifecycleEvent lifecycleEvent = null == setting ?
                new TaskLifecycleEvent(SettingUtils.getWorkspace(), sid, serviceName, TaskLifecycle.EXCEPTION_OFFLINE)
                :
                new TaskLifecycleEvent(setting, TaskLifecycle.EXCEPTION_OFFLINE);

        NotifyReactor.getInstance().publishEvent(lifecycleEvent);

        if (StringUtils.isNotEmpty(afterServerErrorOffline)) {
            String cmd = afterServerErrorOffline + StringUtils.SPACE + serviceName;
            TaskUtils.getTaskExecutor().execute(() -> TaskUtils.startTask(cmd, null, null));
        }

        final SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
        String s = sdf.format(new Date());
        if (null != setting && Boolean.TRUE.equals(setting.getDaemon())) {
            MessageUtils.warn(String.format("服务%s于%s异常退出，即将启动守护启动！", serviceName, s));
            //启动
            TaskUtils.getTaskExecutor().execute(() -> this.startSingleService(setting));
        } else {
            MessageUtils.warn(String.format("服务%s于%s异常退出，请检查服务状态！", serviceName, s));
        }
    }

    @PostConstruct
    public void init() {
        NotifyReactor.getInstance().registerSubscriber(this);
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return ServiceOfflineEvent.class;
    }
}
