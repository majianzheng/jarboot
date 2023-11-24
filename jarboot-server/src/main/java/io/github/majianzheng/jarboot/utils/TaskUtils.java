package io.github.majianzheng.jarboot.utils;

import io.github.majianzheng.jarboot.base.AgentManager;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
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
    /** 是否使用nohup启动服务 */
    private static final boolean USE_NOHUP;

    static {
        //根据CPU核心数计算线程池CoreSize，最小为4，防止为1时造成阻塞
        int coreSize = Math.max(Runtime.getRuntime().availableProcessors(), 4);
        TASK_EXECUTOR = Executors.newScheduledThreadPool(coreSize,
                JarbootThreadFactory.createThreadFactory("jarboot-task-pool"));
        USE_NOHUP = (!Boolean.getBoolean("docker") &&
                (OSUtils.isLinux() || OSUtils.isMac()) &&
                FileUtils.getFile("/usr/bin/nohup").exists());
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
        String serverPath = SettingUtils.getWorkspace() + File.separator + setting.getUserDir() + File.separator + setting.getName();
        String jvm = SettingUtils.getJvm(serverPath, setting.getVm());
        StringBuilder cmdBuilder = new StringBuilder();

        cmdBuilder
                // jvm 配置
                .append(jvm)
                .append(StringUtils.SPACE)
                //忽略字节码校验，提高启动速度；彩色日志启动
                .append("-noverify -Dspring.output.ansi.enabled=always")
                .append(StringUtils.SPACE)
                // Java agent
                .append(SettingUtils.getAgentStartOption(setting.getUserDir(), setting.getName(), sid))
                .append(StringUtils.SPACE);
        if (CommonConst.SHELL_TYPE.equals(setting.getApplicationType())) {
            final String xmx = "-Xmx";
            final String xms = "-Xms";
            if (StringUtils.isNotEmpty(jvm) && !jvm.contains(xmx) && !jvm.contains(xms)) {
                cmdBuilder.append("-Xms5m -Xmx15m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 ");
            }
            cmdBuilder
                    .append("-jar")
                    .append(StringUtils.SPACE)
                    .append(SettingUtils.getToolsJar())
                    .append(StringUtils.SPACE)
                    .append("-c")
                    .append(StringUtils.SPACE)
                    .append(setting.getCommand());
        } else {
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
        }

        // 传入参数
        String startArg = setting.getArgs();
        if (StringUtils.isNotEmpty(startArg)) {
            cmdBuilder.append(StringUtils.SPACE).append(startArg);
        }

        // 工作目录
        String workHome = setting.getWorkDirectory();
        if (StringUtils.isBlank(workHome)) {
            workHome = serverPath;
        } else {
            //解析相对路径或绝对路径，得到真实路径
            workHome = getAbsolutePath(workHome, serverPath);
        }
        String displayCmd;
        if (CommonConst.SHELL_TYPE.equals(setting.getApplicationType())) {
            displayCmd = setting.getCommand();
            if (StringUtils.isNotEmpty(setting.getArgs())) {
                displayCmd = displayCmd + " " + setting.getArgs();
            }
        } else {
            displayCmd = "java " + cmdBuilder.toString();
        }

        //打印命令行
        MessageUtils.console(setting.getUserDir(), sid, displayCmd);
        // 启动、等待启动完成，最长2分钟
        int waitTime = SettingUtils.getSystemSetting().getMaxStartTime();
        final String workDir = workHome;
        final String bashFileExt = OSUtils.isWindows() ? "cmd" : "sh";
        String bashName = String.format("shell_%s.%s", sid, bashFileExt);
        File bashFile = FileUtils.getFile(serverPath, bashName);

        String javaCmd = OSUtils.isWindows() ? "%JAVA_CMD% " : "${JAVA_CMD} ";
        if (USE_NOHUP) {
            javaCmd = "nohup " + javaCmd;
        }
        cmdBuilder.insert(0, javaCmd);
        String cmd = cmdBuilder.toString();
        // java命令
        String jdkPath;
        if (StringUtils.isBlank(setting.getJdkPath())) {
            jdkPath = SettingUtils.getJdkPath();
        } else {
            // 使用了指定到jdk
            jdkPath = getAbsolutePath(setting.getJdkPath(), serverPath);
        }
        AgentManager.getInstance()
                .waitServiceStarted(
                        setting,
                        waitTime,
                        () -> startTask(cmd, setting.getEnv(), workDir, bashFile, jdkPath));
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
     * @param bashFile 临时的bash可执行文件
     * @param jdkPath jdk路径
     */
    public static void startTask(String command, String environment, String workHome, File bashFile, String jdkPath) {
        StringBuilder sb = new StringBuilder();
        try {
            initJdkPath(jdkPath, sb);
            String[] envs = parseEnv(environment);
            if (null != envs) {
                for (String env : envs) {
                    if (OSUtils.isWindows()) {
                        sb.append("set ").append(env).append('\n');
                    } else {
                        sb.append("export ").append(env).append('\n');
                    }
                }
            }
            if (OSUtils.isWindows()) {
                sb.append("\nstart \"\" ").append(command).append("\ntimeout /t 3 > NUL\necho started!\n");
            } else {
                sb.append('\n').append(command).append(" >/dev/null &\nsleep 3\necho started!\n");
            }
            FileUtils.writeStringToFile(bashFile, sb.toString(), StandardCharsets.UTF_8);
            if (!bashFile.setExecutable(true)) {
                logger.error("set executable failed.");
            }
            String bash = bashFile.getAbsolutePath();
            List<String> cmd = OSUtils.isWindows() ? Collections.singletonList(bash) : Arrays.asList("sh", bash);
            new ProcessBuilder(cmd).directory(toCurrentDir(workHome)).start();
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    private static void initJdkPath(String jdkPath, StringBuilder sb) {
        if (StringUtils.isEmpty(jdkPath)) {
            if (OSUtils.isWindows()) {
                sb.append("set \"JAVA_CMD=javaw\"\n\n");
            } else {
                sb.append("export JAVA_CMD=\"java\"\n\n");
            }
        } else {
            if (OSUtils.isWindows()) {
                sb.append("set \"JAVA_HOME=").append(jdkPath).append("\"\n");
                sb.append("set \"JAVA_CMD=%JAVA_HOME%/bin/javaw.exe\"\n\n");
            } else {
                sb.append("export JAVA_HOME=\"").append(jdkPath).append("\"\n");
                sb.append("export JAVA_CMD=\"${JAVA_HOME}/bin/java\"\n\n");
            }
        }
        if (OSUtils.isWindows()) {
            sb.append("set \"JARBOOT_HOME=").append(SettingUtils.getHomePath()).append("\"\n");
        } else {
            sb.append("export JARBOOT_HOME=\"").append(SettingUtils.getHomePath()).append("\"\n");
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
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            MessageUtils.warn(e.getMessage());
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

    private static String[] parseEnv(String environment) {
        String[] en;
        if (StringUtils.isBlank(environment)) {
            en = null;
        } else {
            en = environment.split(CommonConst.COMMA_SPLIT);
        }
        return en;
    }

    private TaskUtils(){}
}
