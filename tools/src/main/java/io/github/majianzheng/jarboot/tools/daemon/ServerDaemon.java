package io.github.majianzheng.jarboot.tools.daemon;

import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.VMUtils;
import io.github.majianzheng.jarboot.tools.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 进程守护
 * @author mazheng
 */
@Slf4j
public class ServerDaemon {
    public static void main(String[] args) {
        String home = Utils.getJarbootHome();
        if (StringUtils.isEmpty(home)) {
            log.error("JARBOOT_HOME is not set!");
            return;
        }
        log.info("启动Jarboot守护进程...");
        try (FileLock daemonLock = CacheDirHelper.singleDaemonTryLock()) {
            if (null == daemonLock) {
                log.error("守护进程(PID: {})已在运行中!", PidFileHelper.getDaemonPid());
                return;
            }
            PidFileHelper.writeDaemonPid();
            CacheDirHelper.getDaemonPidFile().deleteOnExit();
            daemon(daemonLock);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void daemon(FileLock daemonLock) {
        if (OSUtils.isWindows()) {
            // 初始化托盘
            log.info("Windows 托盘应用初始化...");
        }
        // 等待启动
        final int maxWaitSec = 60;
        final int onceWait = 3;
        boolean prepared = false;
        for (int i = 0; i < maxWaitSec; ++i) {
            prepared = isPrepared();
            if (prepared) {
                break;
            }
            try {
                TimeUnit.SECONDS.sleep(onceWait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!prepared) {
            log.error("守护进程等待{}秒后仍未检测到Jarboot服务启动，守护退出！", maxWaitSec*onceWait);
            return;
        }
        long preparedTime = System.currentTimeMillis();
        // 开始监听
        try (FileLock lock = CacheDirHelper.singleInstanceLock()) {
            process(lock, daemonLock, preparedTime);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void process(FileLock lock, FileLock daemonLock, long preparedTime) throws IOException {
        if (null == lock) {
            log.error("单例进程加锁失败！");
            return;
        }

        final long maxWaitTime = 5 * 60 * 1000L;
        if (System.currentTimeMillis() - preparedTime < maxWaitTime) {
            log.error("Jarboot服务启动失败，守护退出！");
            return;
        }
        lock.release();
        daemonLock.release();
        startup(Utils.getJarbootHome());
    }

    private static void startup(String home) {
        // 启动startup.sh
        String [] cmd = OSUtils.isWindows() ? new String[]{"bin/windows/startup.cmd"} : new String[]{"sh", "bin/startup.sh"};
        try {
            Process process = Runtime.getRuntime().exec(cmd, null, FileUtils.getFile(home));
            if (OSUtils.isWindows()) {
                process.getOutputStream().write('\r');
                process.getOutputStream().flush();
            }
            process.waitFor(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static boolean isPrepared() {
        Map<String, String> vms = VMUtils.getInstance().listVM();
        String pid = PidFileHelper.getServerPid();
        if (!vms.containsKey(pid)) {
            log.info("检测到Jarboot服务(PID: {})启动中...", pid);
            return false;
        }
        log.info("Jarboot服务(PID: {})已启动，准备就绪...", pid);
        return Utils.isStarted();
    }
}
