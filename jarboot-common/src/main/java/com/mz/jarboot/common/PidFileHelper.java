package com.mz.jarboot.common;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author majianzheng
 */
public class PidFileHelper {
    private static final String CACHE_DIR = System.getProperty("JARBOOT_HOME") + File.separator + ".cache";
    private static final String PID_DIR = CACHE_DIR + File.separator + "pid";
    private static final String PID_EXT = ".pid";
    private static final int INVALID_PID = -1;
    private static final String PID;
    static {
        String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        int index = name.indexOf('@');
        PID = name.substring(0, index);
    }

    public static void writePidFile(String sid) {
        //写入pid
        File dir = FileUtils.getFile(PID_DIR);
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

    public static int getServerPid(String sid) {
        File pidFile = FileUtils.getFile(PID_DIR, sid + PID_EXT);
        int pid = INVALID_PID;
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
            String content = FileUtils.readFileToString(pidFile, StandardCharsets.UTF_8);
            pid = Integer.parseInt(content);
        } catch (Exception exception) {
            //ignore
        }
        return pid;
    }

    public static String getCurrentPid() {
        return PID;
    }

    public static void deletePidFile(String sid) {
        File pid = FileUtils.getFile(PID_DIR, sid + PID_EXT);
        if (!pid.exists()) {
            return;
        }
        try {
            FileUtils.forceDelete(pid);
        } catch (Exception exception) {
            //ignore
        }
    }

    public static String getPidDir() {
        return PID_DIR;
    }

    private PidFileHelper() {}
}
