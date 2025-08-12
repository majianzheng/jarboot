package io.github.majianzheng.jarboot.tools.shell;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.common.utils.VMUtils;
import io.github.majianzheng.jarboot.tools.common.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 检查服务状态
 * @author mazheng
 */
@Slf4j
public class CheckStatus {
    public static void main(String[] args) {
        if (StringUtils.isEmpty(System.getenv(CommonConst.JARBOOT_HOME))) {
            log.error("JARBOOT_HOME env is not set!");
            return;
        }
        String daemonPid = PidFileHelper.getDaemonPid();
        String serverPid = PidFileHelper.getServerPid();
        Map<String, String> vms = VMUtils.getInstance().listVM();
        String daemonStatus = vms.containsKey(daemonPid) ?
                String.format("%s (PID: %s)", AnsiLog.green("running"), daemonPid) : AnsiLog.red("dead");
        String serverStatus;
        if (vms.containsKey(serverPid)) {
            if (Utils.isStarted()) {
                serverStatus = String.format("%s (PID: %s)", AnsiLog.green("running"), serverPid);
            } else {
                serverStatus = String.format("%s (PID: %s)", AnsiLog.yellow("starting"), serverPid);
            }
        } else {
            serverStatus = AnsiLog.red("dead");
        }

        AnsiLog.println("Status:\n  [daemon]    {}\n  [server]    {}", daemonStatus, serverStatus);
    }
}
