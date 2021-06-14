package com.mz.jarboot.service.impl;

import com.google.common.base.Stopwatch;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.task.TaskRunCache;
import com.mz.jarboot.dto.*;
import com.mz.jarboot.event.TaskEvent;
import com.mz.jarboot.service.ServerMgrService;
import com.mz.jarboot.task.TaskStatus;
import com.mz.jarboot.utils.*;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;

/**
 * 服务管理
 * @author majianzheng
 */
@Service
public class ServerMgrServiceImpl implements ServerMgrService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String START_TIME_CONST = "启动耗时：";

    @Autowired
    private TaskRunCache taskRunCache;

    @Autowired
    private ExecutorService taskExecutor;

    @Override
    public List<ServerRunningDTO> getServerList() {
        return taskRunCache.getServerList();
    }

    /**
     * 一键重启，杀死所有服务进程，根据依赖重启
     */
    @Override
    public void oneClickRestart() {
        if (this.taskRunCache.hasNotFinished()) {
            WebSocketManager.getInstance().notice("一键重启，当前有正在启动或关闭的服务在执行中，请稍后再试", NoticeEnum.ERROR);
            return;
        }
        //获取所有的服务
        List<String> allWebServerList = taskRunCache.getServerNameList();
        //同步控制，保证所有的都杀死后再重启
        if (CollectionUtils.isNotEmpty(allWebServerList)) {
            //启动服务
            this.restartServer(allWebServerList);
        }
    }

    /**
     * 一键启动，根据依赖重启
     */
    @Override
    public void oneClickStart() {
        if (this.taskRunCache.hasNotFinished()) {
            WebSocketManager.getInstance().notice("一键启动，当前有正在启动或关闭的服务在执行中，请稍后再试", NoticeEnum.ERROR);
            return;
        }
        List<String> allWebServerList = taskRunCache.getServerNameList();
        //启动服务
        this.startServer(allWebServerList);
    }

    /**
     * 一键停止，杀死所有服务进程
     */
    @Override
    public void oneClickStop() {
        if (this.taskRunCache.hasNotFinished()) {
            WebSocketManager.getInstance().notice("一键停止，当前有正在启动或关闭的服务在执行中，请稍后再试", NoticeEnum.ERROR);
            return;
        }
        List<String> allWebServerList = taskRunCache.getServerNameList();
        //启动服务
        this.stopServer(allWebServerList);
    }

    /**
     * 启动服务
     *
     * @param servers 服务列表，列表内容为jar包的上级文件夹的名称
     */
    @Override
    public void startServer(List<String> servers) {
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }

        //在线程池中执行，防止前端请求阻塞超时
        taskExecutor.execute(() -> this.startServer0(servers));
    }

    //同步方法，全部完成后返回
    private void startServer0(List<String> servers) {
        //获取服务的优先级启动顺序
        final Queue<ServerSettingDTO> priorityQueue = PropertyFileUtils.parseStartPriority(servers);
        List<ServerSettingDTO> taskList = new ArrayList<>();
        ServerSettingDTO setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting);
            ServerSettingDTO next = priorityQueue.peek();
            if (null != next && !next.getPriority().equals(setting.getPriority())) {
                //同一级别的全部取出
                startServerGroup(taskList);
                //开始指定下一级的启动组，此时上一级的已经全部启动完成，清空组
                taskList.clear();
            }
        }
        //最后一组的启动
        startServerGroup(taskList);
    }

    /**
     * 同一级别的一起启动
     * @param s 同级服务列表
     */
    private void startServerGroup(List<ServerSettingDTO> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(setting ->
                taskExecutor.execute(() -> {
                    this.startSingleServer(setting);
                    countDownLatch.countDown();
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
    private void startSingleServer(ServerSettingDTO setting) {
        String server = setting.getServer();
        if (TaskUtils.isAlive(server)) {
            //已经启动
            this.sendStartedMessage(server, this.taskRunCache.getTaskPid(server));
            WebSocketManager.getInstance().notice("服务" + server + "已经是启动状态", NoticeEnum.INFO);
            return;
        }

        //设定启动中，并发送前端让其转圈圈
        this.taskRunCache.setTaskInfo(server, CommonConst.STATUS_STARTING, CommonConst.INVALID_PID);
        WebSocketManager.getInstance().publishStatus(server, TaskStatus.START);

        //记录开始时间
        Stopwatch stopwatch = Stopwatch.createStarted();
        //开始启动进程
        TaskUtils.startServer(server, setting);
        //记录启动结束时间
        long costTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        int pid = TaskUtils.getServerPid(server);
        //服务是否启动成功
        if (CommonConst.INVALID_PID == pid) {
            //启动失败
            this.taskRunCache.setTaskInfo(server, CommonConst.STATUS_STOPPED, CommonConst.INVALID_PID);
            WebSocketManager.getInstance().publishStatus(server, TaskStatus.START_ERROR);
        } else {
            TaskUtils.attach(server, pid);
            WebSocketManager.getInstance().sendConsole(server,
                    START_TIME_CONST + costTime + "毫秒");
            this.sendStartedMessage(server, pid);
        }
    }

    /**
     * 停止服务
     *
     * @param servers 服务列表，列表内容为jar包的上级文件夹的名称
     */
    @Override
    public void stopServer(List<String> servers) {
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }

        //在线程池中执行，防止前端请求阻塞超时
        taskExecutor.execute(() -> this.stopServer0(servers));
    }

    private void stopServer0(List<String> servers) {
        //获取服务的优先级顺序，与启动相反的顺序依次终止
        final Queue<ServerSettingDTO> priorityQueue = PropertyFileUtils.parseStopPriority(servers);
        List<String> taskList = new ArrayList<>();
        ServerSettingDTO setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting.getServer());
            ServerSettingDTO next = priorityQueue.peek();
            if (null != next && !next.getPriority().equals(setting.getPriority())) {
                //同一级别的全部取出
                stopServerGroup(taskList);
                //开始指定下一级的启动组，此时上一级的已经全部启动完成，清空组
                taskList.clear();
            }
        }
        //最后一组的启动
        stopServerGroup(taskList);
    }

    private void stopServerGroup(List<String> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(server ->
                taskExecutor.execute(() -> {
                    this.stopSingleServer(server);
                    countDownLatch.countDown();
                }));

        try {
            //等待全部终止完成
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopSingleServer(String server) {
        //发送停止中消息
        this.taskRunCache.setTaskInfo(server, CommonConst.STATUS_STOPPING, CommonConst.INVALID_PID);
        WebSocketManager.getInstance().publishStatus(server, TaskStatus.STOP);

        //记录开始时间
        Stopwatch stopwatch = Stopwatch.createStarted();

        TaskUtils.killServer(server);

        //耗时
        long costTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        //停止成功
        if (AgentManager.getInstance().isOnline(server)) {
            this.taskRunCache.setTaskInfo(server, CommonConst.STATUS_RUNNING, CommonConst.INVALID_PID);
            WebSocketManager.getInstance().publishStatus(server, TaskStatus.STOP_ERROR);
        } else {
            WebSocketManager.getInstance().sendConsole(server, "停止成功！耗时：" + costTime + "毫秒");
            this.taskRunCache.setTaskInfo(server, CommonConst.STATUS_STOPPED, CommonConst.INVALID_PID);
            WebSocketManager.getInstance().publishStatus(server, TaskStatus.STOPPED);
        }
    }

    /**
     * 重启服务
     *
     * @param servers 服务列表，列表内容为jar包的上级文件夹的名称
     */
    @Override
    public void restartServer(List<String> servers) {
        //获取终止的顺序
        taskExecutor.execute(() -> {
            //先依次终止
            stopServer0(servers);
            //再依次启动
            startServer0(servers);
        });
    }


    private void sendStartedMessage(String server, int pid) {
        this.taskRunCache.setTaskInfo(server, CommonConst.STATUS_RUNNING, pid);

        WebSocketManager.getInstance().publishStatus(server, TaskStatus.STARTED);
    }

    @EventListener
    public void onTaskEvent(TaskEvent event) {
        switch (event.getEventType()) {
            case RESTART:
                this.restartServer(event.getServices());
                break;
            case DAEMON_START:
                this.startServer(event.getServices());
                break;
            default:
                logger.error("未知的消息类型");
                break;
        }
    }
}
