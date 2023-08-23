package io.github.majianzheng.jarboot.core.cmd.internal;

import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;

/**
 * 进程优雅退出
 * @author majianzheng
 */
public class ExitCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        session.console(EnvironmentContext.getAgentClient().getServiceName() + "即将退出");
        PidFileHelper.deletePidFile(EnvironmentContext.getAgentClient().getSid());
        session.end(true, "Application exiting...");
        System.exit(0);
    }

    @Override
    public boolean notAllowPublicCall() {
        return true;
    }
}
