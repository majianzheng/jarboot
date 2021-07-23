package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;

/**
 * 进程优雅退出
 * @author jianzhengma
 */
public class ExitCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        session.console(EnvironmentContext.getServer() + "即将退出");
        session.ack("即将执行退出");
        session.end();
        System.exit(0);
    }
}
