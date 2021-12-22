package com.mz.jarboot.core.cmd;

import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.core.session.CommandCoreSession;
import com.mz.jarboot.core.session.Completion;
import com.mz.jarboot.core.utils.StringUtils;

/**
 * The command abstract class which defined the common behave.
 * @author majianzheng
 */
public abstract class AbstractCommand {
    protected String name = StringUtils.EMPTY;
    protected CommandCoreSession session;

    /**
     * 命令是否执行中
     * @return 是否执行中
     */
    public boolean isRunning() {
        return null != session && session.isRunning();
    }

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

    public void setSession(CommandCoreSession session) {
        this.session = session;
    }

    public CommandCoreSession getSession() {
        return session;
    }

    /**
     * 取消执行
     */
    public void cancel() {
        session.cancel();
    }

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

    public void printHelp() {
        this.printHelp(this.getClass());
    }

    protected void printHelp(Class<?> cls) {
        if (null == session) {
            return;
        }
        session.console("Usage:" + StringUtils.LF);
        Name cmd = cls.getAnnotation(Name.class);
        if (null != cmd) {
            session.console("Command: " + cmd.value() + StringUtils.LF);
        }
        Summary summary = cls.getAnnotation(Summary.class);
        if (null != summary) {
            session.console(summary.value() + StringUtils.LF);
        }
        Description description = cls.getAnnotation(Description.class);
        if (null != description) {
            session.console(description.value());
        }
    }
}
