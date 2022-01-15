package com.mz.jarboot.utils;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.JarbootThreadFactory;
import com.mz.jarboot.common.utils.OSUtils;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.common.PidFileHelper;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.common.utils.VMUtils;
import com.mz.jarboot.constant.NoticeLevel;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 操作系统任务进程相关工具方法
 *
 * @author majianzheng
 */
public class TaskUtils {
    private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);

    /** 服务启动超时时间 */
    private static int maxStartTime = 12000;
    /** 任务调度线程池 */
    private static final ScheduledExecutorService TASK_EXECUTOR;

    static {
        //根据CPU核心数计算线程池CoreSize，最小为4，防止为1时造成阻塞
        int coreSize = Math.max(Runtime.getRuntime().availableProcessors(), 4);
        TASK_EXECUTOR = Executors.newScheduledThreadPool(coreSize,
                JarbootThreadFactory.createThreadFactory("jarboot-task-pool"));
    }

    /**
     * 获取线程池
     * @return 线程池
     */
    public static ScheduledExecutorService getTaskExecutor() {
        return TASK_EXECUTOR;
    }

    /**
     * 服务最大启动时间
     */
    public static void setMaxStartTime(int v) {
        maxStartTime = v;
    }

    /**
     * 杀死服务进程
     * @param service 服务名
     */
    public static void killService(String service, String sid) {
        //先尝试向目标进程发送停止命令
        boolean isOk = AgentManager.getInstance().killClient(service, sid);

        //检查有没有成功退出，若失败，则执行强制杀死系统命令
        if (!isOk) {
            if (AgentManager.getInstance().isOnline(sid)) {
                logger.warn("未能成功退出，将执行强制杀死命令：{}", service);
                WebSocketManager.getInstance().notice("服务" + service +
                        "未等到退出消息，将执行强制退出命令！", NoticeLevel.WARN);
            }
            String pid = getPid(sid);
            if (!pid.isEmpty()) {
                killByPid(pid);
                PidFileHelper.deletePidFile(sid);
            }
        }
    }

    /**
     * 启动服务进程
     * @param service 服务名
     * @param setting 服务配置
     */
    public static void startService(String service, ServiceSetting setting) {
        //服务目录
        String sid = setting.getSid();
        String serverPath = setting.getWorkspace() + File.separator + setting.getName();
        String jvm = SettingUtils.getJvm(serverPath, setting.getVm());
        StringBuilder cmdBuilder = new StringBuilder();

        // java命令
        if (StringUtils.isBlank(setting.getJdkPath())) {
            cmdBuilder.append(CommonConst.JAVA_CMD);
        } else {
            // 使用了指定到jdk
            String jdkPath = getAbsolutePath(setting.getJdkPath(), serverPath);
            cmdBuilder
                    .append(jdkPath)
                    .append( File.separator)
                    .append(CommonConst.BIN_NAME)
                    .append( File.separator)
                    .append(CommonConst.JAVA_CMD);
            if (OSUtils.isWindows()) {
                cmdBuilder.append(CommonConst.EXE_EXT);
            }
        }
        cmdBuilder
                .append(StringUtils.SPACE)
                // jvm 配置
                .append(jvm)
                .append(StringUtils.SPACE)
                //忽略字节码校验，提高启动速度；彩色日志启动
                .append("-noverify -Dspring.output.ansi.enabled=always")
                .append(StringUtils.SPACE)
                // Java agent
                .append(SettingUtils.getAgentStartOption(service, sid))
                .append(StringUtils.SPACE);
        if (StringUtils.isBlank(setting.getCommand())) {
            //获取启动的jar文件
            String jar = SettingUtils.getJarPath(serverPath);
            if (StringUtils.isBlank(jar)) {
                return;
            }
            // 待执行的jar
            cmdBuilder.append(CommonConst.ARG_JAR).append(jar);
        } else {
            cmdBuilder.append(setting.getCommand());
        }

        // 传入参数
        String startArg = setting.getArgs();
        if (StringUtils.isNotEmpty(startArg)) {
            cmdBuilder.append(StringUtils.SPACE).append(startArg);
        }

        String cmd = cmdBuilder.toString();

        // 工作目录
        String workHome = setting.getWorkDirectory();
        if (StringUtils.isBlank(workHome)) {
            workHome = serverPath;
        } else {
            //解析相对路径或绝对路径，得到真实路径
            workHome = getAbsolutePath(workHome, serverPath);
        }

        //打印命令行
        WebSocketManager.getInstance().sendConsole(sid, cmd);
        // 启动
        startTask(cmd, setting.getEnv(), workHome);
        //等待启动完成，最长2分钟
        AgentManager.getInstance().waitServerStarted(service, sid, maxStartTime);
    }

    /**
     * 通过agent机制附加到目标进程
     * @param sid pid
     */
    public static void attach(String sid) {
        String pid = getPid(sid);
        if (pid.isEmpty()) {
            return;
        }
        Object vm = null;
        try {
            vm = VMUtils.getInstance().attachVM(pid);
            VMUtils.getInstance().loadAgentToVM(vm, SettingUtils.getAgentJar(), SettingUtils.getAttachArgs());
        } catch (Exception e) {
            WebSocketManager.getInstance().printException(sid, e);
        } finally {
            if (null != vm) {
                VMUtils.getInstance().detachVM(vm);
            }
        }
    }

    /**
     * 根据sid获取服务的PID
     * @param sid sid
     * @return PID
     */
    public static String getPid(String sid) {
        String pid = StringUtils.EMPTY;
        try {
            pid = PidFileHelper.getServerPidString(sid);
            if (!pid.isEmpty()) {
                Map<String, String> vms = VMUtils.getInstance().listVM();
                if (!vms.containsKey(pid)) {
                    pid = StringUtils.EMPTY;
                    PidFileHelper.deletePidFile(sid);
                }
            }
        } catch (Exception exception) {
            //ignore
        }
        return pid;
    }

    /**
     * 路径转换
     * @param s 路径
     * @param serverPath 服务路径
     * @return 真实路径
     */
    private static String getAbsolutePath(String s, String serverPath) {
        if (SettingUtils.isAbsolutePath(s)) {
            return s;
        }
        File dir = FileUtils.getFile(serverPath, s);
        if (dir.exists() && dir.isDirectory()) {
            return dir.getPath();
        }
        return serverPath;
    }

    /**
     * 启动进程
     * @param command 命令
     * @param environment 环境变量
     * @param workHome 工作目录
     */
    public static void startTask(String command, String environment, String workHome) {
        String[] en;
        if (StringUtils.isBlank(environment)) {
            en = null;
        } else {
            en = environment.split(CommonConst.COMMA_SPLIT);
        }
        File dir = null;
        if (StringUtils.isNotEmpty(workHome)) {
            dir = new File(workHome);
            if (!dir.exists() || !dir.isDirectory()) {
                dir = null;
            }
        }

        try {
            Runtime.getRuntime().exec(command, en, dir);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice("Start task error " + e.getMessage(), NoticeLevel.ERROR);
        }
    }

    public static String parseCommandSimple(String command) {
        command = command.trim();
        int p = command.indexOf(' ');
        if (p > 0) {
            command = command.substring(0, p);
        }
        int index = Math.max(command.lastIndexOf('/'), command.lastIndexOf('\\'));
        if (-1 != index) {
            command = command.substring(index + 1);
        }
        if (!command.endsWith(CommonConst.JAR_EXT)) {
            index = command.lastIndexOf('.');
            if (-1 != index) {
                command = command.substring(index + 1);
            }
        }
        return command;
    }

    /**
     * 强制杀死进程
     * @param pid 进程PID
     */
    private static void killByPid(String pid) {
        if (pid.isEmpty()) {
            return;
        }
        String cmd = String.format(OSUtils.isWindows() ? "taskkill /F /pid %s" : "kill -9 %s", pid);
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeLevel.WARN);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeLevel.WARN);
        } finally {
            if (null != p) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    private TaskUtils(){}
}
