package com.mz.jarboot.common;

import com.mz.jarboot.api.constant.CommonConst;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * @author majianzheng
 */
public class CacheDirHelper {
    private static final String PID_DIR = "pid";
    private static final String UPLOAD_DIR = "upload-server";
    private static final String TEMP_DIR = "temp";
    private static final String MONITOR_RECORD_DIR = "monitor-record";
    private static final String CACHE_DIR = ".cache";

    /**
     * 获取pid缓存文件夹
     * @return pid文件夹
     */
    public static File getPidDir() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, PID_DIR);
    }

    public static File getUploadTemp() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, UPLOAD_DIR);
    }

    public static File getUploadTempServer(String server) {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, UPLOAD_DIR, server);
    }

    public static File getTempDir(String name) {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, TEMP_DIR, name);
    }

    public static File getMonitorRecordDir() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, MONITOR_RECORD_DIR);
    }

    public static File getMonitorRecordFile(String sid) {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, MONITOR_RECORD_DIR, sid + ".snapshot");
    }

    public static synchronized void init() {
        File cacheDir = FileUtils.getFile(getJarbootHome(), CACHE_DIR);
        if (!cacheDir.exists()) {
            try {
                FileUtils.forceMkdir(cacheDir);
            } catch (Exception e) {
                //ignore
            }
        }
        //启动时清理temp文件
        File tempDir = FileUtils.getFile(CACHE_DIR, TEMP_DIR);
        if (tempDir.exists()) {
            FileUtils.deleteQuietly(tempDir);
        }
        if (OSUtils.isWindows()) {
            //windows系统设为隐藏文件夹
            ExecNativeCmd.exec(new String[]{"attrib", "\"" + cacheDir.getAbsolutePath() + "\"", "+H"});
        }
    }

    private static String getJarbootHome() {
        return System.getProperty(CommonConst.JARBOOT_HOME);
    }

    private CacheDirHelper() {}
}
