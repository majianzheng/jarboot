package com.mz.jarboot.core.cmd;

import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.session.Completion;

/**
 * The command abstract class which defined the common behave.
 * @author majianzheng
 */
public abstract class AbstractCommand {
    protected String name = CoreConstant.EMPTY_STRING;
    protected CommandSession session;

    /**
     * 命令是否执行中
     * @return 是否执行中
     */
    public abstract boolean isRunning();

    public final void setName(String name) {
        this.name = name;
    }

    /**
     * 获取命令名
     * @return 命令名
     */
    public String getName() {
        return name;
    }

    public void setSession(CommandSession session) {
        this.session = session;
    }

    public CommandSession getSession() {
        return session;
    }

    /**
     * 取消执行
     */
    public abstract void cancel();

    /**
     * 命令执行逻辑
     */
    public abstract void run();

    /**
     * 命令补全
     * @param completion 补全内容
     */
    public void complete(Completion completion) {
        // default do nothing
    }
}
