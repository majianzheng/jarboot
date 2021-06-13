package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.Command;

public class CancelCommandImpl extends InternalCommand {
    @Override
    public void run() {
        Command current = EnvironmentContext.getCurrentCommand(session.getSessionId());
        if (null != current && current.isRunning()) {
            current.cancel();
        }
        session.ack("取消执行");
        session.end();
    }
}
