package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.session.CommandSession;

public class CancelCommandImpl extends InternalCommand {
    @Override
    public void run(CommandSession handler) {
        Command current = EnvironmentContext.getCurrentCommand(handler.getSessionId());
        if (null != current && current.isRunning()) {
            current.cancel();
        }
        handler.ack("取消执行");
        handler.end();
    }
}
