package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;

/**
 * 当浏览器客户端退出或者刷新时触发
 * @author majianzheng
 */
public class SessionInvalidCommand extends AbstractInternalCommand {
    @Override
    public void run() {
        EnvironmentContext.releaseSession(session.getSessionId());
        session.ack("Released the invalided session.");
        session.end();
    }
}
