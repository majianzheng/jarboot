package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.AbstractCommand;

/**
 * 命令取消执行
 * @author majianzheng
 */
public class CancelCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        AbstractCommand current = EnvironmentContext.getCurrentCommand(session.getSessionId());
        if (null != current && current.isRunning()) {
            current.cancel();
        }
        session.ack("取消执行");
        session.end();
    }
}
