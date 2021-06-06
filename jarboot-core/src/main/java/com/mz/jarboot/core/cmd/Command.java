package com.mz.jarboot.core.cmd;

import com.mz.jarboot.core.session.CommandSession;

/**
 * The command abstract class which defined the common behave.
 * @author majianzheng
 */
public abstract class Command {
    protected String name = "";
    protected String args = "";
    protected String sessionId = "null";
    public abstract boolean isRunning();

    public final void setName(String name) {
        this.name = name;
    }
    public final void setArgs(String args) {
        this.args = args;
    }
    //命令名称
    public String getName() {return name;}

    //命令参数
    public String getArgs() {
        return args;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public abstract void cancel();

    public abstract void run(CommandSession handler);

    public abstract void complete();
}
