package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.utils.LogUtils;

/**
 * 进程优雅退出
 * @author majianzheng
 */
public class ExitCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        session.console(EnvironmentContext.getServer() + "即将退出");
        LogUtils.deletePidFile(EnvironmentContext.getSid());
        session.end(true, "Application exiting...");
        System.exit(0);
    }
}
