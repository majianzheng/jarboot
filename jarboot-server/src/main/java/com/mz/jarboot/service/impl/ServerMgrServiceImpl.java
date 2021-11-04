package com.mz.jarboot.service.impl;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.task.TaskRunCache;
import com.mz.jarboot.event.TaskEvent;
import com.mz.jarboot.api.service.ServerMgrService;
import com.mz.jarboot.task.TaskStatus;
import com.mz.jarboot.utils.*;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * 服务管理
 * @author majianzheng
 */
@Service
public class ServerMgrServiceImpl implements ServerMgrService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${jarboot.after-server-error-offline:}")
    private String afterServerErrorOffline;

    @Autowired
    private TaskRunCache taskRunCache;

    @Override
    public List<ServerRunning> getServerList() {
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
        List<String> paths = taskRunCache.getServerPathList();
        //同步控制，保证所有的都杀死后再重启
        if (CollectionUtils.isNotEmpty(paths)) {
            //启动服务
            this.restartServer(paths);
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
        List<String> paths = taskRunCache.getServerPathList();
        //启动服务
        this.startServer(paths);
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
        List<String> paths = taskRunCache.getServerPathList();
        //启动服务
        this.stopServer(paths);
    }

    /**
     * 启动服务
     *
     * @param paths 服务列表，字符串格式：服务path
     */
    @Override
    public void startServer(List<String> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }

        //在线程池中执行，防止前端请求阻塞超时
        TaskUtils.getTaskExecutor().execute(() -> this.startServer0(paths));
    }

    private void startServer0(List<String> paths) {
        //获取服务的优先级启动顺序
        final Queue<ServerSetting> priorityQueue = PropertyFileUtils.parseStartPriority(paths);
        ArrayList<ServerSetting> taskList = new ArrayList<>();
        ServerSetting setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting);
            ServerSetting next = priorityQueue.peek();
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
    private void startServerGroup(List<ServerSetting> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(setting ->
                TaskUtils.getTaskExecutor().execute(() -> {
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
    @Override
    public void startSingleServer(ServerSetting setting) {
        String server = setting.getServer();
        String sid = setting.getSid();
        // 已经处于启动中或停止中时不允许执行开始，但是开始中时应当可以执行停止，用于异常情况下强制停止
        if (this.taskRunCache.isStartingOrStopping(sid)) {
            WebSocketManager.getInstance().notice("服务" + server + "正在启动或停止", NoticeEnum.INFO);
            return;
        }
        if (AgentManager.getInstance().isOnline(sid)) {
            //已经启动
            WebSocketManager.getInstance().publishStatus(sid, TaskStatus.STARTED);
            WebSocketManager.getInstance().notice("服务" + server + "已经是启动状态", NoticeEnum.INFO);
            return;
        }

        this.taskRunCache.addStarting(sid);
        try {
            //设定启动中，并发送前端让其转圈圈
            WebSocketManager.getInstance().publishStatus(sid, TaskStatus.START);
            //记录开始时间
            long startTime = System.currentTimeMillis();
            //开始启动进程
            TaskUtils.startServer(server, setting);
            //记录启动结束时间，减去判定时间修正

            double costTime = (System.currentTimeMillis() - startTime)/1000.0f;
            //服务是否启动成功
            if (AgentManager.getInstance().isOnline(sid)) {
                WebSocketManager.getInstance().sendConsole(sid,
                        String.format("%s started cost %.3f second.", server, costTime));
                WebSocketManager.getInstance().publishStatus(sid, TaskStatus.STARTED);
            } else {
                //启动失败
                WebSocketManager.getInstance().publishStatus(sid, TaskStatus.START_ERROR);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.ERROR);
        } finally {
            this.taskRunCache.removeStarting(sid);
        }
    }

    /**
     * 停止服务
     *
     * @param paths 服务列表，字符串格式：服务path
     */
    @Override
    public void stopServer(List<String> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }

        //在线程池中执行，防止前端请求阻塞超时
        TaskUtils.getTaskExecutor().execute(() -> this.stopServer0(paths));
    }

    private void stopServer0(List<String> paths) {
        //获取服务的优先级顺序，与启动相反的顺序依次终止
        final Queue<ServerSetting> priorityQueue = PropertyFileUtils.parseStopPriority(paths);
        ArrayList<ServerSetting> taskList = new ArrayList<>();
        ServerSetting setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting);
            ServerSetting next = priorityQueue.peek();
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

    private void stopServerGroup(List<ServerSetting> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(server ->
                TaskUtils.getTaskExecutor().execute(() -> {
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

    private void stopSingleServer(ServerSetting setting) {
        String server = setting.getServer();
        String sid = setting.getSid();
        if (this.taskRunCache.isStopping(sid)) {
            WebSocketManager.getInstance().notice("服务" + server + "正在停止中", NoticeEnum.INFO);
            return;
        }
        this.taskRunCache.addStopping(sid);
        try {
            //发送停止中消息
            WebSocketManager.getInstance().publishStatus(sid, TaskStatus.STOP);
            //记录开始时间
            long startTime = System.currentTimeMillis();
            TaskUtils.killServer(server, sid);
            //耗时
            double costTime = (System.currentTimeMillis() - startTime)/1000.0f;
            //停止成功
            if (AgentManager.getInstance().isOnline(sid)) {
                WebSocketManager.getInstance().publishStatus(sid, TaskStatus.STOP_ERROR);
            } else {
                WebSocketManager.getInstance().sendConsole(sid,
                        String.format("%s stopped cost %.3f second.", server, costTime));
                WebSocketManager.getInstance().publishStatus(sid, TaskStatus.STOPPED);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.ERROR);
        } finally {
            this.taskRunCache.removeStopping(sid);
        }
    }

    /**
     * 重启服务
     *
     * @param servers 服务列表，字符串格式：服务path
     */
    @Override
    public void restartServer(List<String> servers) {
        //获取终止的顺序
        TaskUtils.getTaskExecutor().execute(() -> {
            //先依次终止
            stopServer0(servers);
            //再依次启动
            startServer0(servers);
        });
    }

    private void onOffline(TaskEvent event) {
        String server = event.getServer();
        String sid = event.getSid();
        //检查进程是否存活
        int pid = TaskUtils.getPid(server, sid);
        if (CommonConst.INVALID_PID != pid) {
            //检查是否处于中间状态
            if (taskRunCache.isStopping(sid)) {
                //处于停止中状态，此时不做干预，守护只针对正在运行的进程
                return;
            }
            //尝试重新初始化代理客户端
            TaskUtils.attach(server, sid);
            return;
        }

        if (StringUtils.isNotEmpty(afterServerErrorOffline)) {
            String cmd = afterServerErrorOffline + StringUtils.SPACE + server;
            TaskUtils.getTaskExecutor().execute(() -> TaskUtils.startTask(cmd, null, null));
        }

        //获取是否开启了守护
        ServerSetting temp = PropertyFileUtils.getServerSettingBySid(sid);
        //检测配置更新
        final ServerSetting setting = null == temp ? null : PropertyFileUtils.getServerSetting(temp.getPath());
        final SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
        String s = sdf.format(new Date());
        if (null != setting && Boolean.TRUE.equals(setting.getDaemon())) {
            WebSocketManager.getInstance().notice(String.format("服务%s于%s异常退出，即将启动守护启动！", server, s)
                    , NoticeEnum.WARN);
            //启动
            TaskUtils.getTaskExecutor().execute(() ->
                    this.startSingleServer(setting));
        } else {
            WebSocketManager.getInstance().notice(String.format("服务%s于%s异常退出，请检查服务状态！", server, s)
                    , NoticeEnum.WARN);
        }
    }

    @EventListener
    public void onTaskEvent(TaskEvent event) {
        switch (event.getEventType()) {
            case RESTART:
                this.restartServer(event.getPaths());
                break;
            case AUTO_START_ALL:
                this.oneClickStart();
                break;
            case OFFLINE:
                this.onOffline(event);
                break;
            default:
                logger.error("未知的消息类型");
                break;
        }
    }
}
