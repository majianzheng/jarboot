package io.github.majianzheng.jarboot.utils;

import io.github.majianzheng.jarboot.api.constant.SettingPropConst;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.VMUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

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
     * 杀死服务进程
     * @param sid 服务sid
     */
    public static void killService(String sid) {
        //先尝试向目标进程发送停止命令
        boolean isOk = AgentManager.getInstance().gracefulExit(sid);

        //检查有没有成功退出，若失败，则执行强制杀死系统命令
        if (!isOk) {
            String pid = getPid(sid);
            if (!pid.isEmpty()) {
                MessageUtils.console(sid, "进程优雅退出失败，将强制杀死进程！");
                killByPid(pid);
                PidFileHelper.deletePidFile(sid);
            }
        }
    }

    /**
     * 启动服务进程
     * @param setting 服务配置
     */
    public static void startService(ServiceSetting setting) {
        //服务目录
        String sid = setting.getSid();
        String serverPath = SettingUtils.getServicePath(setting.getUserDir(), setting.getName());
        StringBuilder cmdBuilder = new StringBuilder();

        cmdBuilder
                .append(StringUtils.SPACE)
                // Java agent
                .append(SettingUtils.getAgentStartOption(setting.getUserDir(), setting.getName(), sid))
                .append(StringUtils.SPACE);
        if (CommonConst.SHELL_TYPE.equals(setting.getApplicationType())) {
            cmdBuilder
                    .append("-Xms50m -Xmx150m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 ")
                    .append("-jar")
                    .append(StringUtils.SPACE)
                    .append('"')
                    .append(getShellJar())
                    .append('"')
                    .append(StringUtils.SPACE)
                    .append("-c")
                    .append(StringUtils.SPACE)
                    .append(setting.getCommand());
        } else {
            // jvm 配置
            String jvm = SettingUtils.getJvm(serverPath, setting.getVm());
            if (StringUtils.isNotEmpty(jvm)) {
                cmdBuilder.append(jvm).append(StringUtils.SPACE);
            }
            if (StringUtils.isBlank(setting.getCommand())) {
                //获取启动的jar文件
                cmdBuilder.append(CommonConst.ARG_JAR)
                        .append('"').append(SettingUtils.getJarPath(serverPath)).append('"');
            } else {
                cmdBuilder.append(setting.getCommand());
            }
        }

        // 传入参数
        String startArg = setting.getArgs();
        if (StringUtils.isNotEmpty(startArg)) {
            cmdBuilder.append(StringUtils.SPACE).append(startArg);
        }
        displayCommand(setting, cmdBuilder, sid);
        // 工作目录
        String workHome = getServiceWorkHome(setting);
        String javaCmd = OSUtils.isWindows() ? "\"%JAVA_CMD%\"" : "\"${JAVA_CMD}\"";
        cmdBuilder.insert(0, javaCmd);
        String cmd = cmdBuilder.toString();
        String jdkPath = getJdkPath(setting, serverPath);
        File bashFile = getStartBashFile(sid, serverPath);
        initServiceEnv(setting, bashFile);
        AgentManager.getInstance()
                .waitServiceStarted(
                        setting,
                        // 启动、等待启动完成，最长2分钟（可配置）
                        SettingUtils.getSystemSetting().getMaxStartTime(),
                        () -> startTask(cmd, setting.getEnv(), workHome, bashFile, jdkPath));
    }

    private static void initServiceEnv(ServiceSetting setting, File bashFile) {
        StringBuilder sb = new StringBuilder();
        String catalinaHome = String.join(File.separator, SettingUtils.getHomePath(), ".cache", "catalina_home");
        if (OSUtils.isWindows()) {
            sb.append("@echo off").append(StringUtils.LINE_BREAK)
                    .append("setlocal enabledelayedexpansion").append(StringUtils.LINE_BREAK).append(StringUtils.LINE_BREAK)
                    .append("set \"SID=").append(setting.getSid()).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"SERVICE_NAME=").append(setting.getName()).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"USER_DIR=").append(setting.getUserDir()).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"SERVICE_APP_TYPE=").append(setting.getApplicationType()).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"SERVICE_PRIORITY=").append(setting.getPriority()).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"SERVICE_DAEMON=").append(setting.getDaemon()).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"CATALINA_BASE=").append(catalinaHome).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"CATALINA_HOME=").append(catalinaHome).append('"').append(StringUtils.LINE_BREAK)
                    .append("set \"SERVICE_SCH_TYPE=").append(setting.getScheduleType()).append('"').append(StringUtils.LINE_BREAK);
        } else {
            sb.append("#!/bin/bash").append(StringUtils.LINE_BREAK).append(StringUtils.LINE_BREAK)
                    .append("export SID=\"").append(setting.getSid()).append('"').append(StringUtils.LINE_BREAK)
                    .append("export SERVICE_NAME=\"").append(setting.getName()).append('"').append(StringUtils.LINE_BREAK)
                    .append("export USER_DIR=\"").append(setting.getUserDir()).append('"').append(StringUtils.LINE_BREAK)
                    .append("export SERVICE_APP_TYPE=\"").append(setting.getApplicationType()).append('"').append(StringUtils.LINE_BREAK)
                    .append("export SERVICE_PRIORITY=\"").append(setting.getPriority()).append('"').append(StringUtils.LINE_BREAK)
                    .append("export SERVICE_DAEMON=\"").append(setting.getDaemon()).append('"').append(StringUtils.LINE_BREAK)
                    .append("export CATALINA_BASE=\"").append(catalinaHome).append('"').append(StringUtils.LINE_BREAK)
                    .append("export CATALINA_HOME=\"").append(catalinaHome).append('"').append(StringUtils.LINE_BREAK)
                    .append("export SERVICE_SCH_TYPE=\"").append(setting.getScheduleType()).append('"').append(StringUtils.LINE_BREAK);
        }
        try {
            FileUtils.writeStringToFile(bashFile, sb.toString(), OSUtils.isWindows() ? "GBK" : "UTF-8");
        } catch (Exception e) {
            logger.error("init service env error.", e);
        }
    }

    private static void displayCommand(ServiceSetting setting, StringBuilder cmdBuilder, String sid) {
        String displayCmd;
        if (CommonConst.SHELL_TYPE.equals(setting.getApplicationType())) {
            displayCmd = setting.getCommand();
            if (StringUtils.isNotEmpty(setting.getArgs())) {
                displayCmd = displayCmd + " " + setting.getArgs();
            }
        } else {
            displayCmd = "java" + cmdBuilder.toString();
        }

        //打印命令行
        MessageUtils.console(sid, displayCmd);
    }

    private static String getJdkPath(ServiceSetting setting, String serverPath) {
        String jdkPath;
        if (StringUtils.isBlank(setting.getJdkPath())) {
            jdkPath = SettingUtils.getJdkPath();
        } else {
            // 使用了指定到jdk
            jdkPath = getAbsolutePath(setting.getJdkPath(), serverPath);
        }
        return jdkPath;
    }

    private static String getServiceWorkHome(ServiceSetting setting) {
        String workHome = setting.getWorkDirectory();
        String serverPath = SettingUtils.getServicePath(setting.getUserDir(), setting.getName());
        if (StringUtils.isBlank(workHome)) {
            workHome = serverPath;
        } else {
            //解析相对路径或绝对路径，得到真实路径
            workHome = getAbsolutePath(workHome, serverPath);
        }
        return workHome;
    }

    private static String getShellJar() {
        return new StringBuilder(CommonUtils.getHomeEnv())
                .append(File.separator)
                .append(CommonConst.COMPONENTS_NAME)
                .append(File.separator)
                .append("jarboot-tools.jar")
                .toString();
    }

    public static void execServiceOfflineShell(ServiceSetting setting) {
        String javaCmd = OSUtils.isWindows() ? "\"%JAVA_CMD%\" " : "\"${JAVA_CMD}\" ";
        String name = setting.getName() + CommonConst.POST_EXCEPTION_TASK_SUFFIX;
        StringBuilder cmdBuilder = new StringBuilder(javaCmd)
                .append("-Xms50m -Xmx150m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 ")
                // Java agent
                .append(SettingUtils.getAgentStartOption(setting.getUserDir(), name, setting.getSid()))
                .append(StringUtils.SPACE)
                .append("-jar")
                .append(StringUtils.SPACE)
                .append('"')
                .append(getShellJar())
                .append('"')
                .append(StringUtils.SPACE)
                .append("-c")
                .append(StringUtils.SPACE)
                .append(SettingUtils.getSystemSetting().getAfterServerOfflineExec());
        String serverPath = SettingUtils.getServicePath(setting.getUserDir(), setting.getName());
        File bashFile = getStartBashFile(setting.getSid(), serverPath);
        String jdkPath = SettingUtils.getJdkPath();
        try {
            initServiceEnv(setting, bashFile);
            startTask(cmdBuilder.toString(), setting.getEnv(), serverPath, bashFile, jdkPath).waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cleanBashFile(serverPath);
        }
    }

    public static void cleanBashFile(String serverPath) {
        File bashFile = getStartBashFile(SettingUtils.createSid(serverPath), serverPath);
        if (bashFile.exists()) {
            FileUtils.deleteQuietly(bashFile);
        }
    }

    private static File getStartBashFile(String sid, String serverPath) {
        final String bashFileExt = OSUtils.isWindows() ? "cmd" : "sh";
        String bashName = String.format("shell_%s.%s", sid, bashFileExt);
        return FileUtils.getFile(serverPath, bashName);
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
            VMUtils.getInstance().loadAgentToVM(vm, SettingUtils.getAgentJar(), SettingUtils.getLocalhost());
        } catch (Exception e) {
            MessageUtils.printException(sid, e);
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
            if (!pid.isEmpty() && (!checkProcessAlive(pid))) {
                pid = StringUtils.EMPTY;
                PidFileHelper.deletePidFile(sid);
            }
        } catch (Exception exception) {
            //ignore
        }
        return pid;
    }

    /**
     * 检查Java进程是否存活
     * @param pid pid
     * @return 是否成功
     */
    public static boolean checkProcessAlive(String pid) {
        Map<String, String> vms = VMUtils.getInstance().listVM();
        return vms.containsKey(pid);
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
     * @param bashFile 临时生成的bash可执行文件
     * @param jdkPath jdk路径
     */
    public static Process startTask(String command, String environment, String workHome, File bashFile, String jdkPath) {
        StringBuilder sb = new StringBuilder();
        try {
            initRunningEnv(jdkPath, sb);
            initEnv(environment, workHome, sb);
            if (OSUtils.isWindows()) {
                sb.append(StringUtils.LINE_BREAK)
                        .append("start \"\" ").append(command).append(StringUtils.LINE_BREAK)
                        .append("timeout /t 1 > NUL").append(StringUtils.LINE_BREAK)
                        .append("echo started!").append(StringUtils.LINE_BREAK);
            } else {
                sb.append(StringUtils.LINE_BREAK)
                        .append(command).append(" >/dev/null &").append(StringUtils.LINE_BREAK)
                        .append("sleep 1").append(StringUtils.LINE_BREAK)
                        .append("echo started!").append(StringUtils.LINE_BREAK);
            }
            FileUtils.writeStringToFile(bashFile, sb.toString(), OSUtils.isWindows() ? "GBK" : "UTF-8", true);
            if (!bashFile.setExecutable(true)) {
                logger.error("set executable failed.");
            }
            String bash = bashFile.getAbsolutePath();
            List<String> cmd = OSUtils.isWindows() ? Collections.singletonList(bash) : Arrays.asList("bash", bash);
            return new ProcessBuilder(cmd).directory(toCurrentDir(workHome)).start();
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    private static void initEnv(String environment, String workHome, StringBuilder sb) {
        List<String> envs = parseEnv(environment);
        parseFromEnvFile(workHome, envs);
        if (!CollectionUtils.isEmpty(envs)) {
            for (String env : envs) {
                if (OSUtils.isWindows()) {
                    sb.append("set ").append(env).append(StringUtils.LINE_BREAK);
                } else {
                    sb.append("export ").append(env).append(StringUtils.LINE_BREAK);
                }
            }
        }
        if (StringUtils.isNotEmpty(workHome)) {
            if (OSUtils.isWindows()) {
                sb.append("set \"WORK_HOME=").append(workHome).append('"').append(StringUtils.LINE_BREAK)
                        .append("cd \"").append("%WORK_HOME%").append('"').append(StringUtils.LINE_BREAK);
            } else {
                sb.append("export WORK_HOME=\"").append(workHome).append('"').append(StringUtils.LINE_BREAK)
                        .append("cd \"").append("$WORK_HOME").append('"').append(StringUtils.LINE_BREAK);
            }
        }
    }

    private static void initRunningEnv(String jdkPath, StringBuilder sb) {
        if (StringUtils.isEmpty(jdkPath)) {
            if (OSUtils.isWindows()) {
                sb.append("set \"JAVA_CMD=javaw\"").append(StringUtils.LINE_BREAK).append(StringUtils.LINE_BREAK);
            } else {
                sb.append("export JAVA_CMD=\"java\"").append(StringUtils.LINE_BREAK).append(StringUtils.LINE_BREAK);
            }
        } else {
            if (OSUtils.isWindows()) {
                sb.append("set \"JAVA_HOME=").append(jdkPath).append('"').append(StringUtils.LINE_BREAK);
                sb.append("set \"JAVA_CMD=%JAVA_HOME%/bin/javaw.exe\"").append(StringUtils.LINE_BREAK).append(StringUtils.LINE_BREAK);
            } else {
                sb.append("export JAVA_HOME=\"").append(jdkPath).append('"').append(StringUtils.LINE_BREAK);
                sb.append("export JAVA_CMD=\"${JAVA_HOME}/bin/java\"").append(StringUtils.LINE_BREAK).append(StringUtils.LINE_BREAK);
            }
        }
        String selfHost = ClusterClientManager.getInstance().getSelfHost();
        String host;
        if (StringUtils.isEmpty(selfHost)) {
            host = SettingUtils.getLocalhost();
        } else {
            host = selfHost;
        }
        if (OSUtils.isWindows()) {
            sb.append("set \"JARBOOT_HOME=").append(SettingUtils.getHomePath()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("set \"MACHINE_CODE=").append(CommonUtils.getMachineCode()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("set \"SERVER_UUID=").append(SettingUtils.getUuid()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("set \"JARBOOT_WORKSPACE=").append(SettingUtils.getWorkspace()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("set \"JARBOOT_HOST=").append(host).append('"').append(StringUtils.LINE_BREAK);
        } else {
            sb.append("export JARBOOT_HOME=\"").append(SettingUtils.getHomePath()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("export MACHINE_CODE=\"").append(CommonUtils.getMachineCode()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("export SERVER_UUID=\"").append(SettingUtils.getUuid()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("export JARBOOT_WORKSPACE=\"").append(SettingUtils.getWorkspace()).append('"').append(StringUtils.LINE_BREAK);
            sb.append("export JARBOOT_HOST=\"").append(host).append('"').append(StringUtils.LINE_BREAK);
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
    public static void killByPid(String pid) {
        if (pid.isEmpty()) {
            return;
        }
        List<String> command = OSUtils.isWindows() ? Arrays.asList("taskkill", "/F", "/pid", pid) : Arrays.asList("kill", "-9", pid);
        try {
            new ProcessBuilder(command).start().waitFor();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            MessageUtils.warn(e.getMessage());
        }
    }

    private static File toCurrentDir(String workHome) {
        File dir = null;
        if (StringUtils.isNotEmpty(workHome)) {
            dir = new File(workHome);
            if (!dir.exists() || !dir.isDirectory()) {
                dir = null;
            }
        }
        return dir;
    }

    private static List<String> parseEnv(String environment) {
        String[] en;
        if (StringUtils.isBlank(environment)) {
            return new ArrayList<>();
        } else {
            en = environment.split(CommonConst.COMMA_SPLIT);
        }
        return List.of(en);
    }

    private static void parseFromEnvFile(String workDir, List<String> envs) {
        File envFile = new File(workDir, SettingPropConst.ENV_FILE);
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith(SettingPropConst.COMMENT_PREFIX) || !line.contains("=")) {
                        continue;
                    }
                    envs.add(line);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private TaskUtils(){}
}
