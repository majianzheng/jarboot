package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.ProcessHandler;

/**
 * @author jianzhengma
 */
public class ExitCommandImpl extends Command {
    public ExitCommandImpl(String name, String args) {
        this.name = name;
    }
    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void cancel() {
        //do nothing
    }

    @Override
    public void run(ProcessHandler handler) {
        handler.console(name, "服务" + EnvironmentContext.getServer() + "即将退出");
        handler.end(name);
        handler.ack(name, "即将执行退出");
        System.exit(0);
    }

    @Override
    public void complete() {
        //do nothing
    }
}
