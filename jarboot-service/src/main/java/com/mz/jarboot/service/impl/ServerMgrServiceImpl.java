package com.mz.jarboot.service.impl;

import com.google.common.base.Stopwatch;
import com.mz.jarboot.constant.ResultCodeConst;
import com.mz.jarboot.constant.SettingConst;
import com.mz.jarboot.dao.TaskRunDao;
import com.mz.jarboot.dto.*;
import com.mz.jarboot.event.TaskEvent;
import com.mz.jarboot.exception.MzException;
import com.mz.jarboot.service.ServerMgrService;
import com.mz.jarboot.utils.*;
import com.mz.jarboot.ws.WebSocketConnManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

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

    @PostConstruct
    public void init() {
        logger.info("根目录：{}", this.rootPath);
    }

    @Override
    public List<ProcDetailDTO> getWebServerList() {
        List<ProcDetailDTO> serverList = new ArrayList<>();
        File[] serviceDirs = getWebServerDirs();
        for (File f : serviceDirs) {
            String server = f.getName();
            if (webServerFilter(f)) {
                ProcDetailDTO p = new ProcDetailDTO();
                p.setName(server);
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
        File[] serviceDirs = getWebServerDirs();
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
            WebSocketConnManager.getInstance().noticeError("一键重启，当前有正在启动或关闭的服务在执行中，请稍后再试");
            return;
        }
        //获取所有的Web服务
        List<String> allWebServerList = getServerNameList();
        //同步控制，保证所有的都杀死后再重启
        if (CollectionUtils.isNotEmpty(allWebServerList)) {
            //启动Web服务
            this.restartServer(allWebServerList);
        }
    }

    /**
     * 一键启动，根据依赖重启
     */
    @Override
    public void oneClickStart() {
        if (this.taskRunDao.hasNotFinished()) {
            WebSocketConnManager.getInstance().noticeError("一键启动，当前有正在启动或关闭的服务在执行中，请稍后再试");
            return;
        }
        List<String> allWebServerList = getServerNameList();
        //启动Web服务
        this.startServer(allWebServerList);
    }

    /**
     * 一键停止，杀死所有服务进程
     */
    @Override
    public void oneClickStop() {
        if (this.taskRunDao.hasNotFinished()) {
            WebSocketConnManager.getInstance().noticeError("一键停止，当前有正在启动或关闭的服务在执行中，请稍后再试");
            return;
        }
        List<String> allWebServerList = getServerNameList();
        //启动Web服务
        this.stopServer(allWebServerList);
    }

    private File[] getWebServerDirs() {
        String servicesPath = this.rootPath + File.separator + SettingConst.SERVICES_DIR;
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
     * 获取web服务的jar包路径
     * @param server 服务名
     * @return jar包路径
     */
    private String getWebServerJarPath(String server) {
        StringBuilder builder = new StringBuilder();
        builder.append(this.rootPath).append(File.separator).append(SettingConst.SERVICES_DIR).
                append(File.separator).append(server);
        File dir = new File(builder.toString());
        if (!dir.isDirectory() || !dir.exists()) {
            logger.error("未找到{}服务的jar包路径{}", server, dir.getPath());
            WebSocketConnManager.getInstance().noticeWarn("未找到服务" + server + "的可执行jar包路径");
        }
        String[] extensions = {"jar"};
        Collection<File> jarList = FileUtils.listFiles(dir, extensions, false);
        if (CollectionUtils.isEmpty(jarList)) {
            logger.error("在{}未找到{}服务的jar包", server, dir.getPath());
            WebSocketConnManager.getInstance().noticeWarn("未找到服务" + server + "的可执行jar包");
        }
        if (jarList.size() > 1) {
            WebSocketConnManager.getInstance().noticeError("在服务目录找到了多个jar包！可能会导致服务不可用，请先清理该目录！留下一个可用的jar包文件！");
        }
        if (jarList.iterator().hasNext()) {
            File jarFile = jarList.iterator().next();
            return jarFile.getPath();
        }
        return "";
    }

    /**
     * 启动服务
     *
     * @param p 服务列表，列表内容为jar包的上级文件夹的名称
     */
    @Override
    public void startServer(List<String> p) {
        if (CollectionUtils.isEmpty(p)) {
            return;
        }
        //引入线程池
        taskExecutor.execute(() -> {
            Deque<List<String>> priorityDeque = PropertyFileUtils.parseStartPriority(p);
            List<String> taskList = priorityDeque.poll();
            while (null != taskList) {
                if (CollectionUtils.isEmpty(taskList)) {
                    break;
                }
                startWebServerGroup(taskList);
                taskList = priorityDeque.poll();
            }
        });
    }
    public void startWebServerGroup(List<String> p) {
        if (CollectionUtils.isEmpty(p)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(p.size());
        p.forEach(server ->
                taskExecutor.execute(() -> {
                    this.startSingleWebServer(server);
                    countDownLatch.countDown();
                }));
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private void startSingleWebServer(String server) {
        if (checkJavaWebProcessAlive(server)) {
            //已经启动
            this.sendStartedMessage(server, this.taskRunDao.getTaskPid(server));
            WebSocketConnManager.getInstance().noticeInfo("Web服务" + server + "已经是启动状态");
            return;
        }
        String jar = getWebServerJarPath(server);
        this.sendStartMessage(server);
        Stopwatch stopwatch = Stopwatch.createStarted();
        TaskUtils.getInstance().startWebServer(jar, text -> this.sendOutMessage(server, text));
        //检查端口，用于确定服务是否启动成功
        if (checkJavaWebProcessAlive(server)) {
            List<Integer> pidList = TaskUtils.getInstance().getJavaPidByName(this.getJarFileName(server));
            this.sendOutMessage(server, START_TIME_CONST + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "毫秒");
            this.sendStartedMessage(server, pidList.get(0));
        } else {
            this.sendStartErrorMessage(server);
        }
    }

    /**
     * 停止服务
     *
     * @param p 服务列表，列表内容为jar包的上级文件夹的名称
     */
    @Override
    public void stopServer(List<String> p) {
        if (CollectionUtils.isEmpty(p)) {
            return;
        }
        p.forEach(server -> taskExecutor.execute(() -> this.stopSingleWebServer(server)));
    }
    private void stopSingleWebServer(String server) {
        String name = this.getJarFileName(server);
        this.sendStopMessage(server, false);
        TaskUtils.getInstance().killJavaByName(name, text -> this.sendOutMessage(server, text));
        //等待2s中
        boolean stopped = false;
        for (int i = 0; i < 15; ++i) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            //停止成功
            if (this.checkJavaWebProcessAlive(server)) {
                TaskUtils.getInstance().killJavaByName(name, text -> this.sendOutMessage(server, text));
            } else {
                stopped = true;
                break;
            }
        }
        //停止成功
        if (stopped) {
            this.sendStopMessage(server, true);
        } else {
            this.sendStopErrorMessage(server);
        }
    }

    private boolean checkJavaWebProcessAlive(String server) {
        return TaskUtils.getInstance().checkAliveByJar(this.getJarFileName(server));
    }
    private String getJarFileName(String server) {
        String jar = this.getWebServerJarPath(server);
        int p = jar.lastIndexOf(File.separatorChar);
        if (-1 != p) {
            jar = jar.substring(p + 1);
        }
        if (File.separatorChar == '\\') {
            jar = server + "\\\\" + jar;
        } else {
            jar = server + File.separatorChar + jar;
        }
        return jar;
    }

    /**
     * 重启服务
     *
     * @param p 服务列表，列表内容为jar包的上级文件夹的名称
     */
    @Override
    public void restartServer(List<String> p) {
        taskExecutor.execute(() -> {
            Deque<List<String>> priorityDeque = PropertyFileUtils.parseStartPriority(p);
            List<String> taskList = priorityDeque.poll();
            while (null != taskList) {
                if (CollectionUtils.isEmpty(taskList)) {
                    break;
                }
                restartWebServerGroup(taskList);
                taskList = priorityDeque.poll();
            }
        });
    }

    private void restartWebServerGroup(List<String> p) {
        if (CollectionUtils.isEmpty(p)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(p.size());
        p.forEach(server ->
                taskExecutor.execute(() -> {
                    this.stopSingleWebServer(server);
                    this.startSingleWebServer(server);
                    countDownLatch.countDown();
                }));
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendOutMessage(String server, String text) {
        WebSocketConnManager.getInstance().sendOutMessage(server, text);
    }
    private void sendStopMessage(String server, boolean finished) {
        String status = finished ? SettingConst.STATUS_STOPPED : SettingConst.STATUS_STOPPING;
        this.taskRunDao.setTaskInfo(server, status, SettingConst.INVALID_PID);

        WebSocketConnManager.getInstance().sendStopMessage(server, finished);
    }
    private void sendStartMessage(String server) {
        this.taskRunDao.setTaskInfo(server, SettingConst.STATUS_STARTING, SettingConst.INVALID_PID);

        WebSocketConnManager.getInstance().sendStartMessage(server);
    }
    private void sendStartedMessage(String server, int pid) {
        this.taskRunDao.setTaskInfo(server, SettingConst.STATUS_RUNNING, pid);

        WebSocketConnManager.getInstance().sendStartedMessage(server, pid);
    }
    private void sendStartErrorMessage(String server) {
        this.taskRunDao.setTaskInfo(server, SettingConst.STATUS_STOPPED, SettingConst.INVALID_PID);

        WebSocketConnManager.getInstance().sendStartErrorMessage(server);
    }
    private void sendStopErrorMessage(String server) {
        this.taskRunDao.setTaskInfo(server, SettingConst.STATUS_RUNNING, SettingConst.INVALID_PID);

        WebSocketConnManager.getInstance().sendStopErrorMessage(server);
    }

    private void updateServerInfo(List<ProcDetailDTO> server) {
        Map<String, String> pidCmdMap = TaskUtils.getInstance().findJavaProcess();
        server.forEach(item -> {
            String pid = pidCmdMap.get(item.getName());
            String status = taskRunDao.getTaskStatus(item.getName());
            if (StringUtils.isEmpty(pid)) {
                item.setStatus(SettingConst.STATUS_STOPPED);
                return;
            }
            item.setPid(pid);
            //未发现ip和端口配置时的运行中的判定
            Date actionTime = taskRunDao.getActionTime(item.getName());
            //点击开始超过60秒，或ebr-setting重启过时，存在pid则判定为已经启动
            if (null == actionTime || ((System.currentTimeMillis() - actionTime.getTime()) > 60000)) {
                item.setStatus(SettingConst.STATUS_RUNNING);
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
                break;
        }
    }
}
