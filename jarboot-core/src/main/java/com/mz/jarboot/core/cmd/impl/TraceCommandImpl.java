package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.session.CommandSession;

public class TraceCommandImpl extends Command {
    private CommandSession handler;
    @Override
    public boolean isRunning() {
        return null != handler && handler.isRunning();
    }

    @Override
    public void cancel() {
        if (null != handler) {
            handler.cancel();
        }
    }

    @Override
    public void run(CommandSession handler) {
        this.handler = handler;
        handler.console("开发中");
    }

    @Override
    public void complete() {
        handler.end();
    }
}
