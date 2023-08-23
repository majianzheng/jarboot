package io.github.majianzheng.jarboot.core.cmd.internal;

import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;

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
            session.end(true, current.getName() + " canceled.");
        } else {
            session.end();
        }
    }
}
