package com.mz.jarboot.utils;

import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.common.OSUtils;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.ServerSettingDTO;
import com.mz.jarboot.event.ApplicationContextUtils;
import com.mz.jarboot.event.NoticeEnum;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.io.FileUtils;
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
    private static long getStartWaitTime() {
        if (-1 == startWaitTime) {
            synchronized (TaskUtils.class) {
                String val = ApplicationContextUtils.getEnv("jarboot.start-wait-time", "5000");
                startWaitTime = NumberUtils.toLong(val, 5000);
                if (startWaitTime < 1500 || startWaitTime > 30000) {
                    startWaitTime = 5000;
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
        return checkAliveByJar(getJarWithServerName(server));
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
            String name = getJarWithServerName(server);
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
        String jvm = SettingUtils.getJvm(server, setting.getJvm());
        if (StringUtils.isEmpty(jvm)) {
            //未配置则获取默认的
            jvm = SettingUtils.getDefaultJvmArg();
        }
        String javaCmd = "java";
        if (StringUtils.isNotEmpty(setting.getJavaHome())) {
            javaCmd = setting.getJavaHome() + File.separator + "bin" + File.separator + "java";
            if (OSUtils.isWindows()) {
                javaCmd += ".exe";
            }
        }
        String cmd = (null == jvm) ?
                String.format("%s -jar %s", javaCmd, jar) :
                String.format("%s %s -jar %s", javaCmd, jvm, jar);
        String startArg = setting.getArgs();
        if (StringUtils.isNotEmpty(startArg)) {
            cmd = String.format("%s %s", cmd, startArg);
        }
        startTask(cmd, setting.getEnvp(), setting.getWorkHome(),
                text -> WebSocketManager.getInstance().sendConsole(server, text));
    }

    /**
     * 获取服务的进程PID
     * @param server 服务名
     * @return 服务PID
     */
    public static int getServerPid(String server) {
        List<Integer> pidList = getJavaPidByName(getJarWithServerName(server));
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
            //ignore
            return;
        }
        try {
            VMUtils.getInstance().loadAgentToVM(vm, SettingUtils.getAgentJar(), SettingUtils.getAgentArgs(server));
        } catch (Exception e) {
            //ignore
        } finally {
            if (null != vm) {
                VMUtils.getInstance().detachVM(vm);
            }
        }
    }

    //得到jar的上级目录和自己: demo-service/demo.jar
    public static String getJarWithServerName(String server) {
        ServerSettingDTO setting = PropertyFileUtils.getServerSetting(server);
        String jar = setting.getJar();
        if (StringUtils.isEmpty(jar)) {
            String path = SettingUtils.getServerPath(server);
            Collection<File> jarFiles = FileUtils.listFiles(new File(path), new String[]{"jar"}, false);
            Iterator<File> iter = jarFiles.iterator();
            if (iter.hasNext()) {
                jar = iter.next().getName();
            } else {
                jar = "";
            }
        }
        return jar;
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
        List<Integer> pidList = new ArrayList<>();
        Process p;
        String cmd = OSUtils.isWindows() ? "cmd /c tasklist |findstr " : "ps -u$USER | grep ";
        cmd += name;
        try {
            p = runtime.exec(cmd);
        } catch (IOException e) {
            return pidList;
        }
        try (InputStream inputStream = p.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))){
            String line;
            while ((line = reader.readLine()) != null) {
                parseLinePid(pidList, line);
            }
        } catch (Exception e) {
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
            index = line.indexOf(' ', index);
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

    public static void startTask(String command, String envp, String workHome, PushMsgCallback callback) {
        Process process;
        String[] en;
        if (StringUtils.isEmpty(envp)) {
            en = null;
        } else {
            en = envp.split(",");
        }
        File dir = null;
        if (StringUtils.isNotEmpty(workHome)) {
            dir = new File(workHome);
        }
        try {
            process = Runtime.getRuntime().exec(command, en, dir);
        } catch (IOException e) {
            return;
        }
        if (null == callback) {
            return;
        }
        final long waitTime = getStartWaitTime();
        try (InputStream inputStream = process.getInputStream()){
            long timestamp = System.currentTimeMillis();
            byte[] buffer = new byte[2048];
            int len;
            for (;;) {
                if (0 == inputStream.available()) {
                    long interval = Math.abs(System.currentTimeMillis() - timestamp);
                    //超过一定时间进程没有输出信息时，认为启动完成
                    if (interval > waitTime) {
                        break;
                    }
                    Thread.sleep(200);//NOSONAR
                } else {
                    //可用
                    len = inputStream.read(buffer);
                    String s = new String(buffer, 0, len);
                    callback.sendMessage(s);
                    timestamp = System.currentTimeMillis();
                }
            }
        } catch (InterruptedException e) {
            callback.sendMessage(e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            callback.sendMessage(e.getLocalizedMessage());
        }
    }

    public static void killByName(String name, PushMsgCallback callback) {
        List<Integer> pid = getPidByName(name);
        if (CollectionUtils.isEmpty(pid)) {
            return;
        }
        killByPid(pid, callback);
    }
    static void killJavaByName(String jar, PushMsgCallback callback) {
        if (StringUtils.isEmpty(jar)) {
            return;
        }
        List<Integer> pidList = getJavaPidByName(jar);
        if (CollectionUtils.isEmpty(pidList)) {
            return;
        }

        pidList.forEach( pid -> killByPid(pid, callback));
    }

    public static boolean checkAliveByJar(String jar) {
        return !CollectionUtils.isEmpty(getJavaPidByName(jar));
    }

    private static List<Integer> getJavaPidByName(String jar) {
        List<Integer> pidList = new ArrayList<>();
        if (StringUtils.isEmpty(jar)) {
            return pidList;
        }
        Map<Integer, String> vms = VMUtils.getInstance().listVM();
        vms.forEach((pid, name) -> {
            if (name.contains(jar)) {
                pidList.add(pid);
            }
        });
        return pidList;
    }

    public static Map<String, Integer> findJavaProcess() {
        Map<String, Integer> pidCmdMap = new HashMap<>();
        Map<Integer, String> vms = VMUtils.getInstance().listVM();
        vms.forEach((pid, name) -> {
            final int p = name.lastIndexOf(File.separatorChar);
            if (p < 1) {
                return;
            }
            int b = name.lastIndexOf(File.separatorChar, p - 1);
            if (b < 0) {
                b = 0;
            } else {
                ++b;
            }
            String serverName = name.substring(b, p);
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
        String cmd = OSUtils.isWindows() ? "taskkill /F /pid " : "kill -9 ";
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd + pid);
            p.waitFor();
            callback.sendMessage("强制终止进程，pid:" + pid);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            WebSocketManager.getInstance().notice(e.getMessage(), NoticeEnum.WARN);
        } catch (IOException e) {
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
    private TaskUtils(){}
}
