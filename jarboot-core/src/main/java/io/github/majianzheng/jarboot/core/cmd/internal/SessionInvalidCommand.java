package io.github.majianzheng.jarboot.core.cmd.internal;

import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;

/**
 * 当浏览器客户端退出或者刷新时触发
 * @author majianzheng
 */
public class SessionInvalidCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        EnvironmentContext.releaseSession(session.getSessionId());
        session.end();
    }

    @Override
    public boolean notAllowPublicCall() {
        return true;
    }
}
