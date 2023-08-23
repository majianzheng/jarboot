package io.github.majianzheng.jarboot.tools.shell;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.VMUtils;

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
