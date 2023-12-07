package io.github.majianzheng.jarboot.core.cmd.internal;

import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.stream.ResultStreamDistributor;

/**
 * 当浏览器客户端退出或者刷新时触发
 * @author majianzheng
 */
public class SessionInvalidCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        cancelCurrent();
        EnvironmentContext.releaseSession(session.getSessionId());
        ResultStreamDistributor.getInstance().removeActiveSession(session.getSessionId());
        session.end();
    }

    private void cancelCurrent() {
        try {
            AbstractCommand current = EnvironmentContext.getCurrentCommand(session.getSessionId());
            if (null != current && current.isRunning()) {
                current.cancel();
                session.end(true, current.getName() + " canceled.");
            } else {
                session.end();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public boolean notAllowPublicCall() {
        return true;
    }
}
