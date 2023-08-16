package io.github.majianzheng.tools.daemon;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.AnsiLog;
import com.mz.jarboot.common.CacheDirHelper;
import com.mz.jarboot.common.PidFileHelper;
import com.mz.jarboot.common.utils.OSUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.common.utils.VMUtils;
import org.apache.commons.io.FileUtils;

import java.nio.channels.FileLock;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 进程守护
 * @author mazheng
 */
public class ServerDaemon {
    public static void main(String[] args) {
        String home = System.getProperty(CommonConst.JARBOOT_HOME);
        if (StringUtils.isEmpty(home)) {
            AnsiLog.error("JARBOOT_HOME is not set!");
            return;
        }
        try (FileLock daemonLock = CacheDirHelper.singleDaemonTryLock()) {
            if (null == daemonLock) {
                AnsiLog.error("守护进程已在运行中!");
                return;
            }
            daemon(home);
        } catch (Exception e) {
            AnsiLog.error(e);
        }
    }

    private static void daemon(String home) {
        if (OSUtils.isWindows()) {
            // 初始化托盘
            AnsiLog.info("Windows 托盘应用初始化...");
        }
        // 等待启动
        final int maxWaitSec = 30;
        boolean prepared = false;
        for (int i = 0; i < maxWaitSec; ++i) {
            prepared = isPrepared();
            if (prepared) {
                break;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!prepared) {
            AnsiLog.error("守护进程等待{}秒后仍未检测到Jarboot服务启动，守护退出！", maxWaitSec);
            return;
        }
        // 开始监听
        try (FileLock lock = CacheDirHelper.singleInstanceLock()) {
            if (lock != null) {
                lock.release();
            }
        } catch (Exception e) {
            AnsiLog.error(e);
        }
        // 启动startup.sh
        String [] cmd = OSUtils.isWindows() ? new String[]{"bin/windows/startup.cmd"} : new String[]{"sh", "bin/startup.sh"};
        try {
            Runtime.getRuntime().exec(cmd, null, FileUtils.getFile(home)).waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        }
    }

    private static boolean isPrepared() {
        boolean prepared = false;
        Map<String, String> vms = VMUtils.getInstance().listVM();
        for (Map.Entry<String, String> entry : vms.entrySet()) {
            if (Objects.equals(PidFileHelper.getServerPid(), entry.getKey())) {
                // 已经加锁并运行
                prepared = true;
                AnsiLog.info("检测到Jarboot服务(PID: {})启动中...", entry.getKey());
                break;
            }
        }
        return prepared;
    }
}