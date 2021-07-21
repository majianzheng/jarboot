package com.mz.jarboot.core.cmd;

import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.session.Completion;

/**
 * The command abstract class which defined the common behave.
 * @author majianzheng
 */
public abstract class Command {
    protected String name = CoreConstant.EMPTY_STRING;
    protected CommandSession session;
    public abstract boolean isRunning();

    public final void setName(String name) {
        this.name = name;
    }
    //命令名称
    public String getName() {
        return name;
    }

    public void setSession(CommandSession session) {
        this.session = session;
    }

    public CommandSession getSession() {
        return session;
    }

    public abstract void cancel();

    public abstract void run();

    public void complete(Completion completion) {
        // default do nothing
    }
}
