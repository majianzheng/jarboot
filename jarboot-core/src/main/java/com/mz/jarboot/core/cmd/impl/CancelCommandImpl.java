package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.ProcessHandler;

public class CancelCommandImpl extends Command {
    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void run(ProcessHandler handler) {
        Command current = EnvironmentContext.getCurrentCommand();
        if (null != current && current.isRunning()) {
            current.cancel();
        }
    }

    @Override
    public void complete() {

    }
}
