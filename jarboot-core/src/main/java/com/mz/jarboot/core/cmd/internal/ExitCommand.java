package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.common.PidFileHelper;
import com.mz.jarboot.core.basic.EnvironmentContext;

/**
 * 进程优雅退出
 * @author majianzheng
 */
public class ExitCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        session.console(EnvironmentContext.getClientData().getServer() + "即将退出");
        PidFileHelper.deletePidFile(EnvironmentContext.getClientData().getSid());
        session.end(true, "Application exiting...");
        System.exit(0);
    }

    @Override
    public boolean notAllowPublicCall() {
        return true;
    }
}
