package com.mz.jarboot.common;

import com.mz.jarboot.api.constant.CommonConst;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * @author majianzheng
 */
public class CacheDirHelper {
    static final String CACHE_DIR;
    static final String PID_DIR;
    static {
        CACHE_DIR = System.getProperty(CommonConst.JARBOOT_HOME) + File.separator + ".cache";
        PID_DIR = CACHE_DIR + File.separator + "pid";
        init();
    }

    /**
     * 获取pid缓存文件夹
     * @return pid文件夹
     */
    public static File getPidDir() {
        return FileUtils.getFile(PID_DIR);
    }

    private static void init() {
        File cacheDir = FileUtils.getFile(CACHE_DIR);
        if (!cacheDir.exists()) {
            try {
                FileUtils.forceMkdir(cacheDir);
                if (OSUtils.isWindows()) {
                    //windows系统设为隐藏文件夹
                    ExecNativeCmd.exec(new String[]{"attrib", "\"" + cacheDir.getAbsolutePath() + "\"", "+H"});
                }
            } catch (Exception e) {
                //ignore
            }
        }
    }

    private CacheDirHelper() {}
}
