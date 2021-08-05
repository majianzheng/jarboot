package com.mz.jarboot.service.impl;

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
        if (this.taskRunCache.hasStartingOrStopping()) {
            // 有任务在中间态，不允许执行
            WebSocketManager.getInstance().notice("存在未完成的任务，请稍后重启", NoticeEnum.INFO);
            return;
        }
        //获取所有的服务
        List<String> serverList = taskRunCache.getServerNameList();
        //同步控制，保证所有的都杀死后再重启
        if (CollectionUtils.isNotEmpty(serverList)) {
            //启动服务
            this.restartServer(serverList);
        }
    }

    /**
     * 一键启动，根据依赖重启
     */
    @Override
    public void oneClickStart() {
        if (this.taskRunCache.hasStartingOrStopping()) {
            // 有任务在中间态，不允许执行
            WebSocketManager.getInstance().notice("存在未完成的任务，请稍后启动", NoticeEnum.INFO);
            return;
        }
        List<String> serverList = taskRunCache.getServerNameList();
        //启动服务
        this.startServer(serverList);
    }

    /**
     * 一键停止，杀死所有服务进程
     */
    @Override
    public void oneClickStop() {
        if (this.taskRunCache.hasStartingOrStopping()) {
            // 有任务在中间态，不允许执行
            WebSocketManager.getInstance().notice("存在未完成的任务，请稍后停止", NoticeEnum.INFO);
            return;
        }
        List<String> serverList = taskRunCache.getServerNameList();
        //启动服务
        this.stopServer(serverList);
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

    private void startServer0(List<String> servers) {
        //获取服务的优先级启动顺序
        final Queue<ServerSettingDTO> priorityQueue = PropertyFileUtils.parseStartPriority(servers);
        ArrayList<ServerSettingDTO> taskList = new ArrayList<>();
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
        // 已经处于启动中或停止中时不允许执行开始，但是开始中时应当可以执行停止，用于异常情况下强制停止
        if (this.taskRunCache.isStartingOrStopping(server)) {
            WebSocketManager.getInstance().notice("服务" + server + "正在启动或停止", NoticeEnum.INFO);
            return;
        }
        if (TaskUtils.isAlive(server)) {
            //已经启动
            WebSocketManager.getInstance().publishStatus(server, TaskStatus.STARTED);
            WebSocketManager.getInstance().notice("服务" + server + "已经是启动状态", NoticeEnum.INFO);
            return;
        }

        this.taskRunCache.addStarting(server);
        try {
            //设定启动中，并发送前端让其转圈圈
            WebSocketManager.getInstance().publishStatus(server, TaskStatus.START);
            //记录开始时间
            long startTime = System.currentTimeMillis();
            //开始启动进程
            TaskUtils.startServer(server, setting);
            //记录启动结束时间，减去判定时间修正

            double costTime = (System.currentTimeMillis() - startTime)/1000.0f;
            int pid = TaskUtils.getServerPid(server);
            //服务是否启动成功
            if (CommonConst.INVALID_PID == pid) {
                //启动失败
                WebSocketManager.getInstance().publishStatus(server, TaskStatus.START_ERROR);
            } else {
                WebSocketManager.getInstance().sendConsole(server,
                        String.format("%s started cost %f second.", server, costTime));
                WebSocketManager.getInstance().publishStatus(server, TaskStatus.STARTED);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.ERROR);
        } finally {
            this.taskRunCache.removeStarting(server);
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
        ArrayList<String> taskList = new ArrayList<>();
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
        if (this.taskRunCache.isStopping(server)) {
            WebSocketManager.getInstance().notice("服务" + server + "正在停止中", NoticeEnum.INFO);
            return;
        }
        this.taskRunCache.addStopping(server);
        try {
            //发送停止中消息
            WebSocketManager.getInstance().publishStatus(server, TaskStatus.STOP);
            //记录开始时间
            long startTime = System.currentTimeMillis();
            TaskUtils.killServer(server);
            //耗时
            double costTime = (System.currentTimeMillis() - startTime)/1000.0f;
            //停止成功
            if (AgentManager.getInstance().isOnline(server)) {
                WebSocketManager.getInstance().publishStatus(server, TaskStatus.STOP_ERROR);
            } else {
                WebSocketManager.getInstance().sendConsole(server,
                        String.format("%s stopped cost %f second.", server, costTime));
                WebSocketManager.getInstance().publishStatus(server, TaskStatus.STOPPED);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.ERROR);
        } finally {
            this.taskRunCache.removeStopping(server);
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

    @EventListener
    public void onTaskEvent(TaskEvent event) {
        switch (event.getEventType()) {
            case RESTART:
                this.restartServer(event.getServices());
                break;
            case DAEMON_START:
                this.startServer(event.getServices());
                break;
            case AUTO_START_ALL:
                this.oneClickStart();
                break;
            default:
                logger.error("未知的消息类型");
                break;
        }
    }
}
