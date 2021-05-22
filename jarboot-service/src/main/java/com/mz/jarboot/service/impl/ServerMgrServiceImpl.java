package com.mz.jarboot.service.impl;

import com.google.common.base.Stopwatch;
import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dao.TaskRunDao;
import com.mz.jarboot.dto.*;
import com.mz.jarboot.event.TaskEvent;
import com.mz.jarboot.exception.MzException;
import com.mz.jarboot.service.ServerMgrService;
import com.mz.jarboot.utils.*;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.io.*;
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
    @Value("${root-path:}")
    private String rootPath;

    @Autowired
    private TaskRunDao taskRunDao;

    @Autowired
    private ExecutorService taskExecutor;

    @Override
    public List<ProcDetailDTO> getWebServerList() {
        List<ProcDetailDTO> serverList = new ArrayList<>();
        File[] serviceDirs = getServerDirs();
        for (File f : serviceDirs) {
            String server = f.getName();
            if (webServerFilter(f)) {
                ProcDetailDTO p = new ProcDetailDTO();
                p.setName(server);
                p.setPath(f.getAbsolutePath());
                serverList.add(p);
            }
        }
        updateServerInfo(serverList);
        return serverList;
    }
    private boolean webServerFilter(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        String[] extensions = {"jar"};
        Collection<File> execJar =  FileUtils.listFiles(dir, extensions, false);
        return 1 == execJar.size();
    }

    private List<String> getServerNameList() {
        File[] serviceDirs = getServerDirs();
        List<String> allWebServerList = new ArrayList<>();
        for (File f : serviceDirs) {
            String server = f.getName();
            if (webServerFilter(f)) {
                allWebServerList.add(server);
            }
        }
        return allWebServerList;
    }

    /**
     * 一键重启，杀死所有服务进程，根据依赖重启
     */
    @Override
    public void oneClickRestart() {
        if (this.taskRunDao.hasNotFinished()) {
            WebSocketManager.getInstance().noticeError("一键重启，当前有正在启动或关闭的服务在执行中，请稍后再试");
            return;
        }
        //获取所有的服务
        List<String> allWebServerList = getServerNameList();
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
        if (this.taskRunDao.hasNotFinished()) {
            WebSocketManager.getInstance().noticeError("一键启动，当前有正在启动或关闭的服务在执行中，请稍后再试");
            return;
        }
        List<String> allWebServerList = getServerNameList();
        //启动服务
        this.startServer(allWebServerList);
    }

    /**
     * 一键停止，杀死所有服务进程
     */
    @Override
    public void oneClickStop() {
        if (this.taskRunDao.hasNotFinished()) {
            WebSocketManager.getInstance().noticeError("一键停止，当前有正在启动或关闭的服务在执行中，请稍后再试");
            return;
        }
        List<String> allWebServerList = getServerNameList();
        //启动服务
        this.stopServer(allWebServerList);
    }

    private File[] getServerDirs() {
        String servicesPath = this.rootPath + File.separator + CommonConst.SERVICES_DIR;
        File servicesDir = new File(servicesPath);
        if (!servicesDir.isDirectory() || !servicesDir.exists()) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, servicesPath + "目录不存在");
        }
        File[] serviceDirs = servicesDir.listFiles((dir, name) -> !StringUtils.equals("lib", name));
        if (null == serviceDirs || serviceDirs.length < 1) {
            throw new MzException(ResultCodeConst.INTERNAL_ERROR, servicesPath + "目录中不存在模块的服务");
        }
        return serviceDirs;
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
            this.sendStartedMessage(server, this.taskRunDao.getTaskPid(server));
            WebSocketManager.getInstance().noticeInfo("服务" + server + "已经是启动状态");
            return;
        }

        //设定启动中，并发送前端让其转圈圈
        this.taskRunDao.setTaskInfo(server, CommonConst.STATUS_STARTING, CommonConst.INVALID_PID);
        WebSocketManager.getInstance().sendStartMessage(server);

        //记录开始时间
        Stopwatch stopwatch = Stopwatch.createStarted();
        //开始启动进程
        TaskUtils.startServer(server, setting);
        //记录启动结束时间
        long end = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        int pid = TaskUtils.getServerPid(server);
        //服务是否启动成功
        if (CommonConst.INVALID_PID == pid) {
            //启动失败
            this.taskRunDao.setTaskInfo(server, CommonConst.STATUS_STOPPED, CommonConst.INVALID_PID);
            WebSocketManager.getInstance().sendStartErrorMessage(server);
        } else {
            WebSocketManager.getInstance().sendOutMessage(server,
                    START_TIME_CONST + end + "毫秒");
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
        this.taskRunDao.setTaskInfo(server, CommonConst.STATUS_STOPPING, CommonConst.INVALID_PID);
        WebSocketManager.getInstance().sendStopMessage(server, false);

        TaskUtils.killServer(server);
        //等待2s中
        boolean stopped = false;
        for (int i = 0; i < 15; ++i) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopped = true;
            }
            //停止成功
            if (TaskUtils.isAlive(server)) {
                TaskUtils.killServer(server);
            } else {
                stopped = true;
                break;
            }
        }
        //停止成功
        if (stopped) {
            this.taskRunDao.setTaskInfo(server, CommonConst.STATUS_STOPPED, CommonConst.INVALID_PID);
            WebSocketManager.getInstance().sendStopMessage(server, true);
        } else {
            this.taskRunDao.setTaskInfo(server, CommonConst.STATUS_RUNNING, CommonConst.INVALID_PID);
            WebSocketManager.getInstance().sendStopErrorMessage(server);
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
        this.taskRunDao.setTaskInfo(server, CommonConst.STATUS_RUNNING, pid);

        WebSocketManager.getInstance().sendStartedMessage(server, pid);
    }

    private void updateServerInfo(List<ProcDetailDTO> server) {
        Map<String, String> pidCmdMap = TaskUtils.findJavaProcess();
        server.forEach(item -> {
            String pid = pidCmdMap.get(item.getName());
            String status = taskRunDao.getTaskStatus(item.getName());
            if (StringUtils.isEmpty(pid)) {
                item.setStatus(CommonConst.STATUS_STOPPED);
                return;
            }
            item.setPid(pid);
            //未发现ip和端口配置时的运行中的判定
            Date actionTime = taskRunDao.getActionTime(item.getName());
            //点击开始超过60秒，或ebr-setting重启过时，存在pid则判定为已经启动
            if (null == actionTime || ((System.currentTimeMillis() - actionTime.getTime()) > 60000)) {
                item.setStatus(CommonConst.STATUS_RUNNING);
                return;
            }
            item.setStatus(status);
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
            default:
                logger.error("未知的消息类型");
                break;
        }
    }
}
