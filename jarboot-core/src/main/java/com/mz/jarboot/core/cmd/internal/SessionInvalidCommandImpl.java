package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.session.CommandSession;

/**
 * 当浏览器客户端退出或者刷新时触发
 */
public class SessionInvalidCommandImpl extends InternalCommand {
    @Override
    public void run(CommandSession handler) {
        EnvironmentContext.releaseSession(handler.getSessionId());
        handler.ack("Released the invalided session.");
        handler.end();
    }
}
