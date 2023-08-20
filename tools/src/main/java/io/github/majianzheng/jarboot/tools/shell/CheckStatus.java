package io.github.majianzheng.jarboot.tools.shell;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.AnsiLog;
import com.mz.jarboot.common.PidFileHelper;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.common.utils.VMUtils;

import java.util.Map;

/**
 * 检查服务状态
 * @author mazheng
 */
public class CheckStatus {
    public static void main(String[] args) {
        if (StringUtils.isEmpty(System.getProperty(CommonConst.JARBOOT_HOME))) {
            AnsiLog.error("JARBOOT_HOME is not set!");
            return;
        }
        String daemonPid = PidFileHelper.getDaemonPid();
        String serverPid = PidFileHelper.getServerPid();
        Map<String, String> vms = VMUtils.getInstance().listVM();
        String daemonStatus = vms.containsKey(daemonPid) ?
                String.format("%s (PID: %s)", AnsiLog.green("running"), daemonPid) : AnsiLog.red("dead");
        String serverStatus = vms.containsKey(serverPid) ?
                String.format("%s (PID: %s)", AnsiLog.green("running"), serverPid) : AnsiLog.red("dead");

        AnsiLog.println("Status:\n  [daemon]    {}\n  [server]    {}", daemonStatus, serverStatus);
    }
}
