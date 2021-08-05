package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.cmd.AbstractCommand;
import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.stream.StdOutStreamReactor;
import com.mz.jarboot.core.utils.StringUtils;

/**
 * 标准输出流开启显示与关闭
 * @author majianzheng
 */
@Name("stdout")
@Summary("Stdout display on web ui")
@Description(CoreConstant.EXAMPLE +
        "  stdout on\n" +
        "  stdout off\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "stdout")
public class StdOutCommand extends AbstractCommand {
    private static final String ACTION_ON = "on";
    private static final String ACTION_OFF = "off";

    private String action;

    @Argument(index = 0, argName = "action", required = false)
    @Description("[action] is \"on\" or \"off\"")
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void run() {
        if (StringUtils.isEmpty(this.action)) {
            boolean flag = StdOutStreamReactor.getInstance().isEnabled();
            String msg = String.format("stdout 状态：%s", flag ? "on" : "off");
            session.end(true, msg);
        } else if (ACTION_ON.equalsIgnoreCase(this.action)) {
            StdOutStreamReactor.getInstance().enabled(true);
        } else if (ACTION_OFF.equalsIgnoreCase(this.action)) {
            StdOutStreamReactor.getInstance().enabled(false);
        } else {
            session.end(false, "stdout on 或 stdout off");
        }
        session.end();
    }
}
