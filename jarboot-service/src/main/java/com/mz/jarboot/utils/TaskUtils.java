package com.mz.jarboot.utils;

import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.constant.CommonConst;
import com.mz.jarboot.dto.ServerSettingDTO;
import com.mz.jarboot.ws.WebSocketManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.websocket.Session;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作系统任务进程相关工具方法
 *
 * @author majianzheng
 */
public class TaskUtils {
    private static final Logger logger = LoggerFactory.getLogger(TaskUtils.class);
    private static final Map<String, ServerSettingDTO> aliveServer = new ConcurrentHashMap<>(64);
    private static final Map<String, Session> onlineServer = new ConcurrentHashMap<>(64);
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
        sendCommand(server, CommandConst.EXIT_CMD, "");

        final Session session = onlineServer.getOrDefault(server, null);
        if (null != session) {
            synchronized (session) {
                try {
                    long b = System.currentTimeMillis();
                    logger.info("等待目标进程的退出事件");
                    //等目标进程发送offline信息时执行notify唤醒当前线程
                    session.wait(CommonConst.MAX_WAIT_EXIT_TIME);
                    logger.info("等待目标进程退出完成,耗时:{} ms", System.currentTimeMillis() - b);
                } catch (InterruptedException e) {
                    //ignore
                    Thread.currentThread().interrupt();
                }
            }
        }
        //检查有没有成功退出，若失败，则执行强制杀死系统命令
        if (isAlive(server)) {
            if (onlineServer.containsKey(server)) {
                logger.warn("未能成功退出，将执行强制杀死命令：{}", server);
                WebSocketManager.getInstance().noticeWarn("服务" + server +
                        "未等到退出消息，将执行强制退出命令！");
            }
            String name = getJarWithServerName(server);
            killJavaByName(name, text -> WebSocketManager.getInstance().sendOutMessage(server, text));
            onlineServer.remove(server);
        }

    }

    /**
     * 启动服务进程
     * @param server 服务名
     * @param setting 服务配置
     */
    public static void startServer(String server, ServerSettingDTO setting) {
        String jar = SettingUtils.getJarPath(server);

        if (StringUtils.isEmpty(jar)) {
            return;
        }
        String jvm = setting.getJvm();
        if (StringUtils.isEmpty(jvm)) {
            jvm = PropertyFileUtils.getCurrentSetting("jvm-arg");
        }
        String agentArgs = SettingUtils.getAgentStartOption(server);
        String cmd = (null == jvm) ?
                String.format("java %s -jar %s", agentArgs, jar) :
                String.format("java %s -jar %s %s", agentArgs, jvm, jar);
        String startArg = setting.getArgs();
        if (StringUtils.isNotEmpty(startArg)) {
            cmd = String.format("%s %s", cmd, startArg);
        }
        startTask(cmd, text -> WebSocketManager.getInstance().sendOutMessage(server, text));
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

    public static void addAliveServer(String server, ServerSettingDTO setting) {
        aliveServer.put(server, setting);
    }

    public static void removeAliveServer(String server) {
        aliveServer.remove(server);
    }

    public static ServerSettingDTO getAliveServerSetting(String server) {
        return aliveServer.getOrDefault(server, null);
    }

    public static void onServerOnline(String server, Session session) {
        onlineServer.put(server, session);
    }

    public static void onServerOffline(String server) {
        final Session session = onlineServer.getOrDefault(server, null);
        if (null == session) {
            return;
        }
        logger.info("目标进程已退出，唤醒killServer方法的执行线程");
        synchronized (session) {
            session.notify();
        }
        onlineServer.remove(server);
    }

    public static Session getOnlineServerSession(String server) {
        return onlineServer.getOrDefault(server, null);
    }

    private static void sendCommand(String server, String cmd, String param) {
        Session session = getOnlineServerSession(server);
        if (null != session) {
            if (!session.isOpen()) {
                onServerOffline(server);
                return;
            }
            JSONObject json = new JSONObject();
            json.put("cmd", cmd);
            json.put("param", param);
            try {
                session.getBasicRemote().sendText(json.toJSONString());
            } catch (Exception e) {
                //ignore
            }
        }
    }

    //得到jar的上级目录和自己: demo-service/demo.jar
    //用于jps -l的输出中找出对应的服务，避免误杀或误判
    private static String getJarWithServerName(String server) {
        String jar = SettingUtils.getJarPath(server);
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
        String cmd = SettingUtils.isWindows() ? "cmd /c tasklist |findstr " : "ps -u$USER | grep ";
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
            WebSocketManager.getInstance().noticeWarn(e.getMessage());
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
        if (SettingUtils.isMacOS()) {
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

    private static void startTask(String command, PushMsgCallback callback) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            return;
        }
        try (InputStream inputStream = process.getInputStream()){
            long timestamp = System.currentTimeMillis();
            byte[] buffer = new byte[2048];
            int len;
            for (;;) {
                if (0 == inputStream.available()) {
                    long interval = Math.abs(System.currentTimeMillis() - timestamp);
                    if (interval > 6000) {
                        break;
                    }
                    Thread.sleep(100);
                } else {
                    //可用
                    len = inputStream.read(buffer);
                    String s = new String(buffer, 0, len);
                    callback.sendMessage(s);
                    timestamp = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
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

    private static boolean checkAliveByJar(String jar) {
        return !CollectionUtils.isEmpty(getJavaPidByName(jar));
    }

    private static List<Integer> getJavaPidByName(String jar) {
        List<Integer> pidList = new ArrayList<>();
        if (StringUtils.isEmpty(jar)) {
            return pidList;
        }
        Map<Integer, String> vms = SettingUtils.listVM();
        vms.forEach((pid, name) -> {
            if (name.contains(jar)) {
                pidList.add(pid);
            }
        });
        return pidList;
    }

    public static Map<String, Integer> findJavaProcess() {
        Map<String, Integer> pidCmdMap = new HashMap<>();
        Map<Integer, String> vms = SettingUtils.listVM();
        vms.forEach((pid, name) -> {
            logger.info("pid:{}, name:{}", pid, name);
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
        String cmd = SettingUtils.isWindows() ? "taskkill /F /pid " : "kill -9 ";
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd + pid);
            p.waitFor();
            callback.sendMessage("强制终止进程，pid:" + pid);
        } catch (Exception e) {
            WebSocketManager.getInstance().noticeWarn(e.getMessage());
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
