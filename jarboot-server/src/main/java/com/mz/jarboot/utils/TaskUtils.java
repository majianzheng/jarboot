package com.mz.jarboot.utils;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.OSUtils;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.ServerSettingDTO;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 操作系统任务进程相关工具方法
 *
 * @author majianzheng
 */
public class TaskUtils {
    private static volatile long startWaitTime = -1;
    private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);
    private static final String ADDER_ARGS_PREFIX = CommonConst.JARBOOT_NAME + CommonConst.DOT + CommonConst.SERVICES + CommonConst.DOT;

    private static long getStartWaitTime() {
        if (-1 == startWaitTime) {
            synchronized (TaskUtils.class) {
                String val = ApplicationContextUtils.getEnv("jarboot.start-wait-time", "5000");
                startWaitTime = NumberUtils.toLong(val, 5000);
                if (startWaitTime < 1500 || startWaitTime > 30000) {
                    startWaitTime = 15000;
                }
            }
        }
        return startWaitTime;
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
            killJavaByName(name, text -> WebSocketManager.getInstance().sendConsole(server, text));
        }

    }

    /**
     * 启动服务进程
     * @param server 服务名
     * @param setting 服务配置
     */
    public static void startServer(String server, ServerSettingDTO setting) {
        //获取启动的jar文件
        String jar = SettingUtils.getJarPath(setting);

        if (StringUtils.isEmpty(jar)) {
            return;
        }
        String jvm = SettingUtils.getJvm(server, setting.getVm());
        if (StringUtils.isEmpty(jvm)) {
            //未配置则获取默认的
            jvm = SettingUtils.getDefaultJvmArg();
        }
        StringBuilder cmdBuilder = new StringBuilder();

        // java命令
        if (StringUtils.isNotEmpty(setting.getJdkPath())) {
            // 使用了指定到jdk
            cmdBuilder
                    .append(setting.getJdkPath())
                    .append( File.separator)
                    .append(CommonConst.BIN_NAME)
                    .append( File.separator)
                    .append(CommonConst.JAVA_CMD);
            if (OSUtils.isWindows()) {
                cmdBuilder.append(CommonConst.EXE_EXT);
            }
        } else {
            cmdBuilder.append(CommonConst.JAVA_CMD);
        }
        // jvm 配置
        if (StringUtils.isBlank(jvm)) {
            cmdBuilder.append(CommonConst.ARG_JAR).append(jar);
        } else {
            cmdBuilder.append(StringUtils.SPACE).append(jvm).append(CommonConst.ARG_JAR).append(jar);
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
            workHome = SettingUtils.getServerPath(server);
        }

        //打印命令行
        WebSocketManager.getInstance().sendConsole(server, cmd);
        // 启动
        startTask(cmd, setting.getEnv(), workHome,
                text -> WebSocketManager.getInstance().sendConsole(server, text));
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

    public interface PushMsgCallback {
        void sendMessage(String text);
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

    public static void startTask(String command, String environment, String workHome, PushMsgCallback callback) {
        String[] en;
        final long waitTime = getStartWaitTime();
        if (StringUtils.isBlank(environment)) {
            en = null;
        } else {
            en = environment.split(CommonConst.COMMA_SPLIT);
        }
        File dir = null;
        if (StringUtils.isNotEmpty(workHome)) {
            dir = new File(workHome);
        }

        String msg = "Finished.";
        try (InputStream inputStream = Runtime.getRuntime().exec(command, en, dir).getInputStream()) {
            if (null == callback) {
                return;
            }
            intervalReadStream(callback, waitTime, inputStream);
        } catch (InterruptedException e) {
            msg = e.getMessage();
            logger.error(msg, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            msg = e.getMessage();
            logger.error(msg, e);
        }
        if (null != callback) {
            callback.sendMessage(msg);
        }
    }

    private static void intervalReadStream(PushMsgCallback callback, long waitTime, InputStream inputStream)
            throws IOException, InterruptedException {
        long timestamp = System.currentTimeMillis();
        byte[] buffer = new byte[4096];
        int len;
        for (;;) {
            if (0 == inputStream.available()) {
                long interval = Math.abs(System.currentTimeMillis() - timestamp);
                //超过一定时间进程没有输出信息时，认为启动完成
                if (interval > waitTime) {
                    break;
                }
                Thread.sleep(100);//NOSONAR
            } else {
                //可用
                len = inputStream.read(buffer);
                String s = new String(buffer, 0, len);
                callback.sendMessage(s);
                timestamp = System.currentTimeMillis();
            }
        }
    }

    public static void killByName(String name, PushMsgCallback callback) {
        List<Integer> pid = getPidByName(name);
        if (CollectionUtils.isEmpty(pid)) {
            return;
        }
        killByPid(pid, callback);
    }
    static void killJavaByName(String name, PushMsgCallback callback) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        List<Integer> pidList = getJavaPidByName(name);
        if (CollectionUtils.isEmpty(pidList)) {
            return;
        }

        pidList.forEach( pid -> killByPid(pid, callback));
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

    public static Map<String, Integer> findJavaProcess() {
        HashMap<String, Integer> pidCmdMap = new HashMap<>();
        Map<Integer, String> vms = VMUtils.getInstance().listVM();
        vms.forEach((pid, name) -> {
            final int p = name.lastIndexOf(ADDER_ARGS_PREFIX);
            if (p < 1) {
                return;
            }
            String serverName = name.substring(p + ADDER_ARGS_PREFIX.length());
            pidCmdMap.put(serverName, pid);
        });
        return pidCmdMap;
    }

    private static void killByPid(List<Integer> pid, PushMsgCallback callback) {
        if (CollectionUtils.isEmpty(pid)) {
            return;
        }
        pid.forEach(p -> killByPid(p, callback));
    }

    private static void killByPid(int pid, PushMsgCallback callback) {
        killByPid(String.valueOf(pid), callback);
    }

    private static void killByPid(String pid, PushMsgCallback callback) {
        String cmd = String.format(OSUtils.isWindows() ? "taskkill /F /pid %s" : "kill -9 %s", pid);
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            callback.sendMessage("强制终止进程，pid:" + pid);
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
    private static String getAfterArgs(String server) {
        return ADDER_ARGS_PREFIX + server;
    }
    private TaskUtils(){}
}
