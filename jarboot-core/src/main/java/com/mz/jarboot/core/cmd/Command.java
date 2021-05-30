package com.mz.jarboot.core.cmd;

/**
 * The command abstract class which defined the common behave.
 * @author majianzheng
 */
public abstract class Command {
    protected String name = "";
    public abstract boolean isRunning();
    //命令名称
    public String name() {return name;}

    //命令参数
    public String args() {
        return null;
    }

    public abstract void cancel();

    public abstract void run(ProcessHandler handler);

    public abstract void complete();
}
