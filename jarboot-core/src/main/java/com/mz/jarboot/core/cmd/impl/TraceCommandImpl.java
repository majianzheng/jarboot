package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.ProcessHandler;

public class TraceCommandImpl extends Command {
    private ProcessHandler handler;
    @Override
    public boolean isRunning() {
        return null != handler && !handler.isEnded() && !handler.isCancel();
    }

    @Override
    public void cancel() {
        if (null != handler) {
            handler.cancel();
        }
    }

    @Override
    public void run(ProcessHandler handler) {
        this.handler = handler;
        handler.console("开发中");
    }

    @Override
    public void complete() {
        handler.end();
    }
}
