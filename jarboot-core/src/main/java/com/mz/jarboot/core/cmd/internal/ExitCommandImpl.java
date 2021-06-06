package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.session.CommandSession;

/**
 * @author jianzhengma
 */
public class ExitCommandImpl extends InternalCommand {
    @Override
    public void run(CommandSession handler) {
        handler.console("服务" + EnvironmentContext.getServer() + "即将退出");
        handler.ack("即将执行退出");
        handler.end();
        System.exit(0);
    }
}
