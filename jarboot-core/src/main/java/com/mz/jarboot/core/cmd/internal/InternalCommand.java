package com.mz.jarboot.core.cmd.internal;

import com.mz.jarboot.core.cmd.Command;

/**
 * 内部命令体系，用于jarboot-service与agent之间交互的专用通道，即时任务，无状态控制
 * @author jianzhengma
 */
public abstract class InternalCommand extends Command {
    @Override
    public final boolean isRunning() {
        return false;
    }

    @Override
    public final void cancel() {
        //do nothing
    }

    @Override
    public abstract void run();
}
