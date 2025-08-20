package io.github.majianzheng.jarboot.common;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author majianzheng
 */
@SuppressWarnings({"java:S2095"})
public class CacheDirHelper {
    private static final String PID_DIR = "pid";
    private static final String UPLOAD_DIR = "upload-server";
    private static final String TEMP_DIR = "temp";
    private static final String MONITOR_RECORD_DIR = "monitor-record";
    private static final String CACHE_DIR = ".cache";
    private static final String SERVER_LOCK = "jarboot.lock";
    private static final int MAX_LEVEL = 15;

    /**
     * 获取缓存文件夹
     * @return 缓存文件夹
     */
    public static File getCacheDir() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR);
    }

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
                AnsiLog.error("单例锁访问失败！");
                return null;
            }
            return fileChannel.tryLock();
        } catch (Exception e) {
            AnsiLog.error("单例进程尝试访问文件锁失败");
            AnsiLog.error(e);
            return null;
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
            AnsiLog.error("单例进程访问文件锁失败");
            AnsiLog.error(e);
        }
        return null;
    }

    public static FileLock singleDaemonTryLock() {
        try {
            FileChannel fileChannel =  getSingleInstanceLockFileChannel("daemon.lock");
            if (null == fileChannel) {
                AnsiLog.error("守护进程单例锁访问失败！");
                return null;
            }
            return fileChannel.tryLock();
        } catch (Exception e) {
            AnsiLog.error("守护进程访问文件锁失败");
            AnsiLog.error(e);
            return null;
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
        File tempDir = FileUtils.getFile(cacheDir, TEMP_DIR);
        if (tempDir.exists()) {
            FileUtils.deleteQuietly(tempDir);
        }
        if (OSUtils.isWindows()) {
            //windows系统设为隐藏文件夹
            ExecNativeCmd.exec(new String[]{"attrib", "\"" + cacheDir.getAbsolutePath() + "\"", "+H"});
        }
    }

    public static File getServerPidFile() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, "jarboot.pid");
    }

    public static File getServerInfoFile() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, "jarboot_server.inf");
    }


    public static File getDaemonPidFile() {
        return FileUtils.getFile(getJarbootHome(), CACHE_DIR, "daemon.pid");
    }

    public static void clean() {
        File cacheDir = FileUtils.getFile(getJarbootHome(), CACHE_DIR);
        File[] allFiles = cacheDir.listFiles();
        if (null == allFiles) {
            return;
        }
        cleanTempFile(allFiles);
        File bashTemp = FileUtils.getFile(cacheDir, "bash_temp");
        if (bashTemp.exists()) {
            File[] files = bashTemp.listFiles();
            if (null != files) {
                for (File file : files) {
                    FileUtils.deleteQuietly(file);
                }
            }
        }
    }

    private static void cleanTempFile(File[] allFiles) {
        // jna-103658734
        File jnaDir = null;
        // tomcat-docbase.9899.12142748640606892693
        // tomcat.9088.9967532467414851425
        // jlinenative-3.25.1-2a85276cf6d59d9c-jlinenative
        // jansi-2.4.1-d7b98e381885acbe-jansi
        List<File> oldFiles = new ArrayList<>();
        final long timeBefore = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L;
        HashSet<String> skipFile  = new HashSet<>(Arrays.asList("jarboot_server.inf", "bash_temp", "catalina_home", MONITOR_RECORD_DIR, "pid", "secretKey16"));
        for (File file : allFiles) {
            String name = file.getName();
            if (skipFile.contains(name) || name.endsWith(".pid") || name.endsWith(".lock") || name.startsWith("pty4j")) {
                continue;
            }
            if (name.startsWith("jna-")) {
                jnaDir = compareAndGetNew(file, jnaDir, oldFiles);
            } else if (name.startsWith("tomcat-docbase.")) {
                FileUtils.deleteQuietly(file);
            } else if (name.startsWith("tomcat.")) {
                FileUtils.deleteQuietly(file);
            } else if (name.startsWith("jlinenative-")) {
                FileUtils.deleteQuietly(file);
            } else if (name.startsWith("jansi-")) {
                FileUtils.deleteQuietly(file);
            } else {
                cleanOldFile(file, timeBefore, 0);
            }
        }

        oldFiles.forEach(FileUtils::deleteQuietly);
    }

    private static void cleanOldFile(File dir, long timeBefore, int level) {
        if (dir.isDirectory() && level < MAX_LEVEL) {
            File[] files = dir.listFiles();
            if (null == files) {
                return;
            }
            for (File f : files) {
                cleanOldFile(f, timeBefore, level + 1);
            }
            files = dir.listFiles();
            if (null == files || files.length == 0) {
                FileUtils.deleteQuietly(dir);
            }
            return;
        }
        if (dir.lastModified() < timeBefore) {
            FileUtils.deleteQuietly(dir);
        }
    }

    private static File compareAndGetNew(File file, File cur, List<File> oldFiles) {
        if (null == cur) {
            cur = file;
        } else {
            if (file.lastModified() > cur.lastModified()) {
                oldFiles.add(cur);
                cur = file;
            } else {
                oldFiles.add(file);
            }
        }
        return cur;
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
                AnsiLog.error("创建单例进程文件锁失败");
                AnsiLog.error(e);
                return null;
            }
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            return raf.getChannel();
        } catch (Exception e) {
            AnsiLog.error("单例进程访问文件锁失败");
            AnsiLog.error(e);
        }
        return null;
    }

    private static String getJarbootHome() {
        return System.getProperty(CommonConst.JARBOOT_HOME, System.getenv(CommonConst.JARBOOT_HOME));
    }

    private CacheDirHelper() {}
}
