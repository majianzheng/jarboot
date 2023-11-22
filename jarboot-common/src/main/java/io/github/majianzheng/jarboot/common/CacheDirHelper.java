package io.github.majianzheng.jarboot.common;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * @author majianzheng
 */
public class CacheDirHelper {
    private static final String PID_DIR = "pid";
    private static final String UPLOAD_DIR = "upload-server";
    private static final String TEMP_DIR = "temp";
    private static final String MONITOR_RECORD_DIR = "monitor-record";
    private static final String CACHE_DIR = ".cache";
    private static final String SERVER_LOCK = "jarboot-server.lock";

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

    public static File getTempBashDir() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, "bash_temp");
    }

    public static File getMonitorRecordDir() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, MONITOR_RECORD_DIR);
    }

    public static File getMonitorRecordFile(String sid) {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, MONITOR_RECORD_DIR, sid + ".snapshot");
    }

    public static FileLock singleInstanceTryLock() {
        try {
            FileChannel fileChannel =  getSingleInstanceLockFileChannel(SERVER_LOCK);
            if (null == fileChannel) {
                throw new JarbootRunException("单例锁访问失败！");
            }
            FileLock lock = fileChannel.tryLock();
            if (null == lock) {
                throw new JarbootRunException("当前已有实例在运行中！");
            }
            return lock;
        } catch (Exception e) {
            AnsiLog.error("单例进程尝试访问文件锁失败: {}", e.getMessage());
            AnsiLog.error(e);
            throw new JarbootRunException("单实例加锁失败！");
        }
    }

    public static FileLock singleInstanceLock() {
        try {
            FileChannel fileChannel =  getSingleInstanceLockFileChannel(SERVER_LOCK);
            if (null == fileChannel) {
                return null;
            }
            return fileChannel.lock();
        } catch (Exception e) {
            AnsiLog.error("单例进程访问文件锁失败: {}", e.getMessage());
            AnsiLog.error(e);
        }
        return null;
    }

    public static FileLock singleDaemonTryLock() {
        try {
            FileChannel fileChannel =  getSingleInstanceLockFileChannel("daemon.lock");
            if (null == fileChannel) {
                throw new JarbootRunException("单例锁访问失败！");
            }
            FileLock lock = fileChannel.tryLock();
            if (null == lock) {
                throw new JarbootRunException("当前已有实例在运行中！");
            }
            return lock;
        } catch (Exception e) {
            AnsiLog.error("守护进程访问文件锁失败: {}", e.getMessage());
            AnsiLog.error(e);
            throw new JarbootRunException("单实例加锁失败！");
        }
    }

    public static File getAesKeyFile() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, "secretKey16");
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
        File bashTemp = getTempBashDir();
        if (!bashTemp.exists()) {
            try {
                FileUtils.forceMkdir(bashTemp);
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

    static File getServerPidFile() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, "jarboot.pid");
    }

    static File getDaemonPidFile() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, "daemon.pid");
    }

    private static FileChannel getSingleInstanceLockFileChannel(String file) {
        File lockFile = FileUtils.getFile(getJarbootHome(), CACHE_DIR, file);
        if (!lockFile.exists()) {
            try {
                if (!lockFile.getParentFile().exists()) {
                    FileUtils.forceMkdir(lockFile.getParentFile());
                }
                if (!lockFile.createNewFile()) {
                    AnsiLog.error("创建单例进程文件锁失败, {}", lockFile.getAbsolutePath());
                    return null;
                }
            } catch (Exception e) {
                AnsiLog.error("创建单例进程文件锁失败: {}", e.getMessage());
                AnsiLog.error(e);
                return null;
            }
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            return raf.getChannel();
        } catch (Exception e) {
            AnsiLog.error("单例进程访问文件锁失败: {}", e.getMessage());
            AnsiLog.error(e);
        }
        return null;
    }

    private static String getJarbootHome() {
        return System.getProperty(CommonConst.JARBOOT_HOME);
    }

    private CacheDirHelper() {}
}
