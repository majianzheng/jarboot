package io.github.majianzheng.jarboot.core.cmd.internal;

import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;

/**
 * 内部命令体系，用于jarboot-service与agent之间交互的专用通道，即时任务，无状态控制
 * @author majianzheng
 */
public abstract class AbstractInternalCommand extends AbstractCommand {
    @Override
    public final boolean isRunning() {
        return false;
    }

    @Override
    public final void cancel() {
        //do nothing
    }

    /**
     * 命令执行逻辑
     */
    @Override
    public abstract void run();

    /**
     * @return 是否允许开放命令执行
     */
    public boolean notAllowPublicCall() {
        return false;
    }
}
