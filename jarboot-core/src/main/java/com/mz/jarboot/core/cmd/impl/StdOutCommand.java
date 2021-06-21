package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.Command;
import com.mz.jarboot.core.cmd.annotation.Argument;
import com.mz.jarboot.core.cmd.annotation.Description;
import com.mz.jarboot.core.cmd.annotation.Name;
import com.mz.jarboot.core.cmd.annotation.Summary;
import com.mz.jarboot.core.cmd.model.SysPropModel;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.stream.StdOutStreamReactor;
import com.mz.jarboot.core.utils.StringUtils;

/**
 * 标准输出流开启显示与关闭
 * @author jianzhengma
 */
@Name("stdout")
@Summary("Stdout display on web ui")
@Description(CoreConstant.EXAMPLE +
        "  stdout on\n" +
        "  stdout off\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "stdout")
public class StdOutCommand extends Command {
    private static final String ACTION_ON = "on";
    private static final String ACTION_OFF = "off";

    private String action;

    @Argument(index = 0, argName = "action", required = true)
    @Description("[action] is \"on\" or \"off\"")
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public boolean isRunning() {
        return null != session && session.isRunning();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void cancel() {
        //do nothing
    }

    @Override
    public void run() {
        if (ACTION_ON.equalsIgnoreCase(this.action)) {
            StdOutStreamReactor.getInstance().register(this.getSession());
        } else if (ACTION_OFF.equalsIgnoreCase(this.action)) {
            StdOutStreamReactor.getInstance().unRegister(this.getSession().getSessionId());
        } else {
            session.end(false, "stdout on 或 stdout off");
        }
        session.end();
    }

    @Override
    public void complete() {
        session.end();
    }
}
