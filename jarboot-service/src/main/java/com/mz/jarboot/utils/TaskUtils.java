package com.mz.jarboot.utils;

import com.mz.jarboot.constant.SettingConst;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.util.CollectionUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskUtils {
    private PushMsgCallback callback = null;
    private Process process;

    public static TaskUtils getInstance() {
        return new TaskUtils();
    }
    public interface ExitLineCallback {
        boolean check(String text, Process p);
    }
    public interface PushMsgCallback {
        void sendMessage(String text);
    }
    public Process getTaskProcess() {
        return this.process;
    }

    /**
     * 根据名称获取进程pid
     * @param name 名称
     * @return pid
     */
    public List<Integer> getPidByName(String name) {
        Runtime runtime = Runtime.getRuntime();
        List<Integer> pidList = new ArrayList<>();
        Process p = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            //TODO Linux待实现
            p = runtime.exec("cmd /c tasklist |findstr " + name);
            inputStream = p.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                //连续空格替换单空格
                parseLinePid(pidList, line);
            }
        } catch (Exception e) {
            this.dispatchCallback(e.getLocalizedMessage());
        } finally {
            if (null != p) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        return pidList;
    }

    private void parseLinePid(List<Integer> pidList, String line) {
        line = line.replaceAll("\\s{2,}", " ");
        String[] a = line.split(" ");
        if (a.length > 2) {
            int pid = NumberUtils.toInt(a[1], -1);
            if (pid > 0) {
                pidList.add(pid);
            }
        }
    }

    public void startTask(String command, String[] envs, String dir, PushMsgCallback callback, ExitLineCallback c) {
        this.callback = callback;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader reader = null;
        InputStream inputStream = null;
        try {
            this.process = StringUtils.isEmpty(dir) ? runtime.exec(command, envs) :
                    runtime.exec(command, envs, new File(dir));
            if (null == c) {
                return;
            }
            inputStream = this.process.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while((line = reader.readLine()) != null){
                dispatchCallback(line);
                if (c.check(line, this.process)) {
                    break;
                }
            }
        } catch (Exception e) {
            this.dispatchCallback(e.getLocalizedMessage());
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    public void startWebServer(String jar, PushMsgCallback callback) {
        if (StringUtils.isEmpty(jar)) {
            return;
        }
        this.callback = callback;
        String jvm = this.getJvmArg(jar);
        if (StringUtils.contains(jar, "ebr-setting-web")) {
            jvm = jvm.replace("encoding=UTF-8", "encoding=GB18030");
        }
        String cmd = (null == jvm) ?
                String.format("java -jar %s", jar) :
                String.format("java -jar %s %s", jvm, jar);
        String startArg = getStartArg(jar);
        if (StringUtils.isNotEmpty(startArg)) {
            cmd = String.format("%s %s", cmd, startArg);
        }
        startTask(cmd, null, null, callback, (line, p) -> {
                    if (StringUtils.contains(line, SettingConst.JVM_STARTED_TAG)) {
                        return true;
                    }
                    if (StringUtils.contains(line, "APPLICATION FAILED TO START")) {
                        p.destroy();
                        return true;
                    }
                    return false;
                }
        );
    }
    public void startWebServerX86(String java, String jar, PushMsgCallback callback) {
        if (StringUtils.isEmpty(jar)) {
            return;
        }
        this.callback = callback;
        String jvm = this.getJvmArg(jar);
        String cmd = (null == jvm) ?
                String.format("%s -jar %s", java, jar) :
                String.format("%s -jar %s %s", java, jvm, jar);
        String startArg = getStartArg(jar);
        if (StringUtils.isNotEmpty(startArg)) {
            cmd = String.format("%s %s", cmd, startArg);
        }
        startTask(cmd, null, null, callback, (line, p) -> {
                    if (StringUtils.contains(line, SettingConst.JVM_STARTED_TAG)) {
                        return true;
                    }
                    if (StringUtils.contains(line, "APPLICATION FAILED TO START")) {
                        p.destroy();
                        return true;
                    }
                    return false;
                }
        );
    }
    private String getJvmArg(String jar) {
        String jvm = PropertyFileUtils.getCurrentSetting("jvm-arg");
        int p = jar.lastIndexOf(File.separatorChar);
        if (-1 == p) {
            return jvm;
        }
        String server = jar.substring(0, p);
        p = server.lastIndexOf(File.separatorChar);
        if (-1 == p) {
            return jvm;
        }
        server = server.substring(p + 1);

        //指定了特定的启动参数
        String s = PropertyFileUtils.getCurrentSetting("jvm-arg." + server);
        if (StringUtils.isNotEmpty(s)) {
            return s;
        }

        Integer xmx = PropertyFileUtils.getXmxByModule(server);
        if (null != xmx) {
            jvm = jvm.replaceFirst("-Xmx[0-9]{1,}m", String.format("-Xmx%dm", xmx));
        }
        Integer xms = PropertyFileUtils.getXmsByModule(server);
        if (null != xms) {
            jvm = jvm.replaceFirst("-Xms[0-9]{1,}m", String.format("-Xmx%dm", xms));
        }
        return jvm;
    }
    private String getStartArg(String jar) {
        int p = jar.lastIndexOf(File.separatorChar);
        if (-1 == p) {
            return "";
        }
        String server = jar.substring(0, p);
        p = server.lastIndexOf(File.separatorChar);
        if (-1 == p) {
            return "";
        }
        server = server.substring(p + 1);

        //指定了特定的启动参数
        String startArg = PropertyFileUtils.getCurrentSetting("start-arg." + server);
        return null == startArg ? "" : startArg;
    }
    public void killByName(String name, PushMsgCallback callback) {
        List<Integer> pid = this.getPidByName(name);
        if (CollectionUtils.isEmpty(pid)) {
            return;
        }
        this.callback = callback;
        this.killByPid(pid);
    }
    public void killJavaByName(String jar, PushMsgCallback callback) {
        if (StringUtils.isEmpty(jar)) {
            return;
        }
        this.callback = callback;
        List<Integer> pidList = getJavaPidByName(jar);
        if (CollectionUtils.isEmpty(pidList)) {
            return;
        }

        pidList.forEach(this::killByPid);
    }

    public boolean checkAliveByJar(String jar) {
        return !CollectionUtils.isEmpty(this.getJavaPidByName(jar));
    }

    public List<Integer> getJavaPidByName(String jar) {
        List<Integer> pidList = new ArrayList<>();
        if (StringUtils.isEmpty(jar)) {
            return pidList;
        }
        Runtime runtime = Runtime.getRuntime();
        Process p = null;
        BufferedReader reader = null;
        InputStream inputStream = null;
        String cmd = SettingUtils.isWindows() ? ("cmd /c jps -l |findstr " + jar) : ("jps -l |grep " + jar);
        try {
            //查找进程号
            p = runtime.exec(cmd);
            inputStream = p.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while((line = reader.readLine()) != null){
                line = line.trim();
                String[] s = line.split(" ");
                int pid = NumberUtils.toInt(s[0], -1);
                if (pid > 0) {
                    pidList.add(pid);
                }
            }
        } catch (Exception e) {
            dispatchCallback(e.getLocalizedMessage());
        } finally {
            if (null != p) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        return pidList;
    }

    public Map<String, String> findJavaProcess() {
        Map<String, String> pidCmdMap = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        Process p = null;
        BufferedReader reader = null;
        InputStream inputStream = null;
        String cmd = SettingUtils.isWindows() ? ("cmd /c jps -l") : ("jps -l");
        try {
            //查找进程号
            p = runtime.exec(cmd);
            inputStream = p.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while((line = reader.readLine()) != null){
                parseJpsCmd(pidCmdMap, line);
            }
        } catch (Exception e) {
            dispatchCallback(e.getLocalizedMessage());
        } finally {
            if (null != p) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        return pidCmdMap;
    }

    private void parseJpsCmd(Map<String, String> pidCmdMap, String line) {
        line = line.trim();
        String[] s = line.split(" ");
        if (s.length < 2) {
            return;
        }
        String command = s[1];
        int p1 = command.lastIndexOf(File.separatorChar);
        if (-1 == p1) {
            return;
        }
        command = command.substring(0, p1);
        p1 = command.lastIndexOf(File.separatorChar);
        if (-1 != p1) {
            command = command.substring(p1 + 1);
            pidCmdMap.put(command, s[0]);
        }
    }

    public void killByPort(String port, PushMsgCallback callback){
        if (StringUtils.isEmpty(port)) {
            return;
        }
        this.callback = callback;
        Runtime runtime = Runtime.getRuntime();
        Process p = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            //查找进程号
            //TODO Linux待实现
            p = runtime.exec("cmd /c netstat -ano | findstr " + port);
            inputStream = p.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            Set<Integer> pidSet = parseWindowsLine(port, reader);
            if (!CollectionUtils.isEmpty(pidSet)) {
                pidSet.forEach(p1 -> this.killByPid(p1.toString()));
            }
        } catch (IOException e) {
            this.dispatchCallback(e.getLocalizedMessage());
        } finally {
            if (null != p) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    private Set<Integer> parseWindowsLine(String port, BufferedReader reader) throws IOException {
        String line;
        List<String> read = new ArrayList<>();
        Set<Integer> pidSet = new HashSet<>();
        while((line = reader.readLine()) != null){
            boolean validPort = validPort(line, port);
            if(validPort){
                read.add(line);
            }
        }
        if(CollectionUtils.isEmpty(read)){
            return pidSet;
        }
        for (String line1 : read) {
            int offset = line1.lastIndexOf(' ');
            String spid = line1.substring(offset);
            spid = spid.replace(" ", "");
            try {
                int pid = Integer.parseInt(spid);
                if (pid > 0) {
                    pidSet.add(pid);
                }
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        return pidSet;
    }

    /**
     * 根据端口号获取进程的PID
     * @param port 端口
     * @return 进程的PID
     */
    public int getPidByPort(String port) {
        if (StringUtils.isEmpty(port)) {
            return -1;
        }
        Runtime runtime = Runtime.getRuntime();
        Process p = null;
        BufferedReader reader = null;
        InputStream inputStream = null;
        try {
            //查找进程号
            //TODO Linux待实现
            p = runtime.exec("cmd /c netstat -ano | findstr " + port);
            inputStream = p.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            Set<Integer> pidSet = parseWindowsLine(port, reader);

            if (!CollectionUtils.isEmpty(pidSet)) {
                return pidSet.iterator().next();
            }
        } catch (IOException e) {
            dispatchCallback(e.getLocalizedMessage());
        } finally {
            if (null != p) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        return -1;
    }

    /**
     * 验证此行是否为指定的端口，因为 findstr命令会是把包含的找出来，例如查找80端口，但是会把8099查找出来
     * @param str 行
     * @return 是否是输入的端口行
     */
    private boolean validPort(String str, String port) {
        Pattern pattern = Pattern.compile("^ *[a-zA-Z]+ +\\S+");
        Matcher matcher = pattern.matcher(str);
        if(!matcher.find()) {
            return false;
        }
        String find = matcher.group();
        int upstart = find.lastIndexOf(':');
        find = find.substring(upstart + 1);

        try {
            Integer.parseInt(find);
        } catch (NumberFormatException e) {
            return false;
        }
        return StringUtils.equals(port, find);
    }
    public void killByPid(List<Integer> pid) {
        if (CollectionUtils.isEmpty(pid)) {
            return;
        }
        pid.forEach(this::killByPid);
    }
    public void killByPid(int pid) {
        killByPid(String.valueOf(pid));
    }
    public void killByPid(String pid) {
        String cmd;
        if (SettingUtils.isWindows()) {
            cmd = "taskkill /F /pid ";
        } else {
            cmd = "kill -9 ";
        }
        Process p = null;
        InputStream inputStream = null;
        try {
            p = Runtime.getRuntime().exec(cmd + pid);
            inputStream = p.getInputStream();
            String txt = readTxt(inputStream, "GBK");
            dispatchCallback(txt);
        } catch (IOException e) {
            dispatchCallback(e.getLocalizedMessage());
        } finally {
            if (null != p) {
                try {
                    p.destroy();
                } catch (Exception e) {
                    //ignore
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    public String readTxt(InputStream in, String charset) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in, charset));
            String line;
            while((line = reader.readLine()) != null){
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            dispatchCallback(e.getLocalizedMessage());
        } finally {
            try {
                if (null != reader) {
                    reader.close();
                }
            } catch (IOException e) {
                //ignore
            }
        }
        return sb.toString();
    }
    private void dispatchCallback(String text) {
        if (null != this.callback) {
            callback.sendMessage(text);
        }
    }
    private TaskUtils(){}
}
