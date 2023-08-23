package io.github.majianzheng.jarboot.common;

import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author majianzheng
 */
public class PidFileHelper {
    private static final String PID_EXT = ".pid";
    public static final String PID;
    public static final String INSTANCE_NAME;
    static {
        INSTANCE_NAME = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        int index = INSTANCE_NAME.indexOf('@');
        PID = INSTANCE_NAME.substring(0, index);
    }

    public static void writePidFile(String sid) {
        //写入pid
        File dir = CacheDirHelper.getPidDir();
        File pidFile = FileUtils.getFile(dir, sid + PID_EXT);
        try {
            if (!dir.exists()) {
                FileUtils.forceMkdir(dir);
            }
            FileUtils.writeStringToFile(pidFile, PID, StandardCharsets.UTF_8);
        } catch (IOException e) {
            //ignore
        }
    }

    public static List<String> getAllSid() {
        File dir = CacheDirHelper.getPidDir();
        String[] pidFileNames = dir.list();
        List<String> sidList = new ArrayList<>();
        if (null == pidFileNames) {
            return sidList;
        }
        for (String name : pidFileNames) {
            if (name.endsWith(PID_EXT)) {
                String sid = name.replace(PID_EXT, StringUtils.EMPTY);
                sidList.add(sid);
            }
        }
        return sidList;
    }

    public static String getServerPidString(String sid) {
        File pidFile = FileUtils.getFile(CacheDirHelper.getPidDir(), sid + PID_EXT);
        String pid = StringUtils.EMPTY;
        if (!pidFile.exists()) {
            return pid;
        }
        if (!pidFile.isFile()) {
            //若不是文件类型，则清除该不合法的文件夹
            try {
                FileUtils.forceDelete(pidFile);
            } catch (Exception exception) {
                //ignore
                return pid;
            }
        }
        try {
            return FileUtils.readFileToString(pidFile, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            //ignore
        }
        return pid;
    }

    public static void deletePidFile(String sid) {
        File pid = FileUtils.getFile(CacheDirHelper.getPidDir(), sid + PID_EXT);
        if (!pid.exists()) {
            return;
        }
        try {
            FileUtils.forceDelete(pid);
        } catch (Exception exception) {
            //ignore
        }
    }

    public static void writeServerPid() {
        try {
            FileUtils.writeStringToFile(CacheDirHelper.getServerPidFile(), PID, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JarbootRunException(e);
        }
    }

    public static void writeDaemonPid() {
        try {
            FileUtils.writeStringToFile(CacheDirHelper.getDaemonPidFile(), PID, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JarbootRunException(e);
        }
    }

    public static String getServerPid() {
        try {
            return FileUtils.readFileToString(CacheDirHelper.getServerPidFile(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // ignore
        }
        return StringUtils.EMPTY;
    }

    public static String getDaemonPid() {
        try {
            return FileUtils.readFileToString(CacheDirHelper.getDaemonPidFile(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // ignore
        }
        return StringUtils.EMPTY;
    }

    private PidFileHelper() {}
}
