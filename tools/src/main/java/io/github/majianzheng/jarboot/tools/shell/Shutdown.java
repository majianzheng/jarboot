package io.github.majianzheng.jarboot.tools.shell;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.AnsiLog;
import com.mz.jarboot.common.PidFileHelper;
import com.mz.jarboot.common.utils.OSUtils;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.common.utils.VMUtils;

import java.util.Map;

/**
 * 终止服务
 * @author mazheng
 */
public class Shutdown {
    public static void main(String[] args) {
        if (StringUtils.isEmpty(System.getProperty(CommonConst.JARBOOT_HOME))) {
            AnsiLog.error("JARBOOT_HOME is not set!");
            return;
        }
        // 先终止守护服务
        String pid = PidFileHelper.getDaemonPid();
        shutdown(pid, "daemon");
        pid = PidFileHelper.getServerPid();
        shutdown(pid, "server");
    }

    private static void shutdown(String pid, String name) {
        if (StringUtils.isEmpty(pid)) {
            AnsiLog.info("Find {} process is not started.", name);
        } else {
            Map<String, String> vms = VMUtils.getInstance().listVM();
            if (vms.containsKey(pid)) {
                AnsiLog.info("Find {} process {} ,killing {}...", name, name, pid);
                kill(pid);
                AnsiLog.info("Send shutdown request to jarboot {}({}) OK", name, pid);
            } else {
                AnsiLog.info("Find {} process is not running.", name);
            }
        }
    }

    private static void kill(String pid) {
        if (StringUtils.isEmpty(pid)) {
            return;
        }
        String[] command = OSUtils.isWindows() ? new String[] {"taskkill", "/F", "/pid", pid} : new String[] {"kill", pid};
        try {
            new ProcessBuilder().command(command).start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        }
    }
}
