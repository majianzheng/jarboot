package com.mz.jarboot.utils;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.JarbootThreadFactory;
import com.mz.jarboot.common.OSUtils;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 操作系统任务进程相关工具方法
 *
 * @author majianzheng
 */
public class TaskUtils {
    private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);
    private static final String ADDER_ARGS_PREFIX = CommonConst.JARBOOT_NAME + CommonConst.DOT +
            "%x" + CommonConst.DOT;
    private static int maxStartTime = 12000;
    private static final ExecutorService TASK_EXECUTOR;
    static {
        ArrayBlockingQueue<Runnable> taskBlockingQueue = new ArrayBlockingQueue<>(256);
        TASK_EXECUTOR = new ThreadPoolExecutor(8, 32,
                32L, TimeUnit.SECONDS, taskBlockingQueue,
                JarbootThreadFactory.createThreadFactory("jarboot-task-pool"),
                //线程池忙碌拒绝策略
                (Runnable r, ThreadPoolExecutor executor) ->
                        WebSocketManager.getInstance().notice("服务器忙碌中，请稍后再试！", NoticeEnum.WARN));
    }

    public static ExecutorService getTaskExecutor() {
        return TASK_EXECUTOR;
    }

    /**
     * 服务最大启动时间
     */
    public static void setMaxStartTime(int v) {
        maxStartTime = v;
    }

    /**
     * 检查服务进程是否存活
     * @param server 服务名
     * @return 是否存活
     */
    public static boolean isAlive(String server) {
        return AgentManager.getInstance().isOnline(server) || checkAliveByJar(getAfterArgs(server));
    }

    /**
     * 杀死服务进程
     * @param server 服务名
     */
    public static void killServer(String server) {
        //先尝试向目标进程发送停止命令
        boolean isOk = AgentManager.getInstance().killClient(server);

        //检查有没有成功退出，若失败，则执行强制杀死系统命令
        if (!isOk && isAlive(server)) {
            if (AgentManager.getInstance().isOnline(server)) {
                logger.warn("未能成功退出，将执行强制杀死命令：{}", server);
                WebSocketManager.getInstance().notice("服务" + server +
                        "未等到退出消息，将执行强制退出命令！", NoticeEnum.WARN);
            }
            String name = getAfterArgs(server);
            killJavaByName(name);
        }

    }

    /**
     * 启动服务进程
     * @param server 服务名
     * @param setting 服务配置
     */
    public static void startServer(String server, ServerSetting setting) {
        //服务目录
        String serverPath = SettingUtils.getServerPath(server);

        String jvm = SettingUtils.getJvm(server, setting.getVm());
        if (StringUtils.isEmpty(jvm)) {
            //未配置则获取默认的
            jvm = SettingUtils.getDefaultJvmArg() + StringUtils.SPACE;
        }
        StringBuilder cmdBuilder = new StringBuilder();

        // java命令
        if (StringUtils.isBlank(setting.getJdkPath())) {
            cmdBuilder.append(CommonConst.JAVA_CMD);
        } else {
            // 使用了指定到jdk
            String jdkPath = getAbsPath(setting.getJdkPath(), serverPath);
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
                // Java agent
                .append(SettingUtils.getAgentStartOption(server))
                .append(StringUtils.SPACE);
        if (Boolean.TRUE.equals(setting.getRunnable())) {
            //获取启动的jar文件
            String jar = SettingUtils.getJarPath(setting);
            if (StringUtils.isBlank(jar)) {
                return;
            }
            // 待执行的jar
            cmdBuilder.append(CommonConst.ARG_JAR).append(jar);
        } else {
            if (StringUtils.isBlank(setting.getUserDefineRunArgument())) {
                return;
            }
            cmdBuilder.append(setting.getUserDefineRunArgument());
        }

        // 传入参数
        String startArg = setting.getArgs();
        if (StringUtils.isNotEmpty(startArg)) {
            cmdBuilder.append(StringUtils.SPACE).append(startArg);
        }

        // 进程标识
        cmdBuilder.append(StringUtils.SPACE).append(getAfterArgs(server));

        String cmd = cmdBuilder.toString();

        // 工作目录
        String workHome = setting.getWorkDirectory();
        if (StringUtils.isBlank(workHome)) {
            workHome = serverPath;
        } else {
            //解析相对路径或绝对路径，得到真实路径
            workHome = getAbsPath(workHome, serverPath);
        }

        //打印命令行
        WebSocketManager.getInstance().sendConsole(server, cmd);
        // 启动
        startTask(cmd, setting.getEnv(), workHome);
        //等待启动完成，最长2分钟
        AgentManager.getInstance().waitServerStarted(server, maxStartTime);
    }

    /**
     * 获取服务的进程PID
     * @param server 服务名
     * @return 服务PID
     */
    public static int getServerPid(String server) {
        List<Integer> pidList = getJavaPidByName(getAfterArgs(server));
        if (CollectionUtils.isEmpty(pidList)) {
            return CommonConst.INVALID_PID;
        }
        return pidList.get(0);
    }

    /**
     * 通过agent机制附加到目标进程
     * @param server 服务名
     * @param pid pid
     */
    public static void attach(String server, int pid) {
        Object vm;
        try {
            vm = VMUtils.getInstance().attachVM(pid);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return;
        }
        try {
            VMUtils.getInstance().loadAgentToVM(vm, SettingUtils.getAgentJar(), SettingUtils.getAgentArgs(server));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != vm) {
                VMUtils.getInstance().detachVM(vm);
            }
        }
    }

    private static String getAbsPath(String s, String serverPath) {
        Path path = Paths.get(s);
        if (path.isAbsolute()) {
            return s;
        }
        File dir = FileUtils.getFile(serverPath, s);
        if (dir.exists() && dir.isDirectory()) {
            return dir.getPath();
        }
        return serverPath;
    }

    /**
     * 根据名称获取进程pid
     * @param name 名称
     * @return pid
     */
    private static List<Integer> getPidByName(String name) {
        Runtime runtime = Runtime.getRuntime();
        ArrayList<Integer> pidList = new ArrayList<>();
        Process p;
        String cmd = OSUtils.isWindows() ? "cmd /c tasklist |findstr " : "ps -u$USER | grep ";
        cmd += name;
        try {
            p = runtime.exec(cmd);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return pidList;
        }
        try (InputStream inputStream = p.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))){
            String line;
            while ((line = reader.readLine()) != null) {
                parseLinePid(pidList, line);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.WARN);
        }
        p.destroy();

        return pidList;
    }

    private static void parseLinePid(List<Integer> pidList, String line) {
        //Linux use ps -u$USER command, and windows use tasklist.
        //Linux line: " 5122 ?        00:00:00 test"
        //MacOS line: 第一段为UID 第二段数字为PID
        //Windows line: "test.exe                    644 Services                   0      1,584 K"
        //下列算法将取出第一段数字部分
        int index;
        int length = line.length();
        for (index = 0; index < length; ++index) {
            char ch = line.charAt(index);
            if (!Character.isSpaceChar(ch)) {
                break;
            }
        }
        if (OSUtils.isMac()) {
            //MacOS取第二段数字部分
            index = line.indexOf(StringUtils.SPACE, index);
        }

        StringBuilder builder = new StringBuilder();
        for(; index < length; ++index) {
            char ch = line.charAt(index);
            if(Character.isDigit(ch)) {
                break;
            }
        }
        for(; index < length; ++index) {
            char ch = line.charAt(index);
            if(Character.isDigit(ch)) {
                builder.append(ch);
            } else {
                break;
            }
        }
        pidList.add(Integer.parseInt(builder.toString()));
    }

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
            WebSocketManager.getInstance().notice("Start task error " + e.getMessage(), NoticeEnum.ERROR);
        }
    }

    public static void killByName(String name) {
        List<Integer> pid = getPidByName(name);
        if (CollectionUtils.isEmpty(pid)) {
            return;
        }
        killByPid(pid);
    }
    static void killJavaByName(String name) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        List<Integer> pidList = getJavaPidByName(name);
        if (CollectionUtils.isEmpty(pidList)) {
            return;
        }
        killByPid(pidList);
    }

    public static boolean checkAliveByJar(String jar) {
        return !CollectionUtils.isEmpty(getJavaPidByName(jar));
    }

    private static List<Integer> getJavaPidByName(String str) {
        ArrayList<Integer> pidList = new ArrayList<>();
        if (StringUtils.isEmpty(str)) {
            return pidList;
        }
        Map<Integer, String> vms = VMUtils.getInstance().listVM();
        vms.forEach((pid, name) -> {
            if (name.contains(str)) {
                pidList.add(pid);
            }
        });
        return pidList;
    }

    public static Map<String, Integer> findProcess() {
        HashMap<String, Integer> pidCmdMap = new HashMap<>(32);
        Map<Integer, String> vms = VMUtils.getInstance().listVM();
        String prefix = getAdderArgsPrefix();
        vms.forEach((pid, name) -> {
            final int p = name.lastIndexOf(prefix);
            if (p < 1) {
                return;
            }
            String serverName = name.substring(p + prefix.length());
            pidCmdMap.put(serverName, pid);
        });
        return pidCmdMap;
    }

    private static void killByPid(List<Integer> pid) {
        if (CollectionUtils.isEmpty(pid)) {
            return;
        }
        pid.forEach(TaskUtils::killByPid);
    }

    private static void killByPid(int pid) {
        killByPid(String.valueOf(pid));
    }

    private static void killByPid(String pid) {
        String cmd = String.format(OSUtils.isWindows() ? "taskkill /F /pid %s" : "kill -9 %s", pid);
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.WARN);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.WARN);
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

    private static String getAdderArgsPrefix() {
        int hash = SettingUtils.getServicesPath().hashCode();
        return String.format(ADDER_ARGS_PREFIX, hash);
    }

    private static String getAfterArgs(String server) {
        return getAdderArgsPrefix() + server;
    }
    private TaskUtils(){}
}
