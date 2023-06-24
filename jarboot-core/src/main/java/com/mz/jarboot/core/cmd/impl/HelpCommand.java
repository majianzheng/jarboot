package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.api.cmd.annotation.*;
import com.mz.jarboot.core.cmd.AbstractCommand;
import com.mz.jarboot.core.cmd.CommandBuilder;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.text.ui.TableElement;
import com.mz.jarboot.text.util.RenderUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * @author majianzheng
 */
@Name("help")
@Summary("Display Jarboot Command Help")
@Description("Examples:\n" + " help\n" + " help sc\n" + " help sm\n" + " help watch")
public class HelpCommand extends AbstractCommand {

    private String cmd;

    @Argument(index = 0, argName = "cmd", required = false)
    @Description("command name")
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public void run() {
        Class<?> definition = CommandBuilder.getCommandDefineClass(cmd);
        if (null == definition) {
            showAll();
        } else {
            showCommandHelp(definition);
        }
        session.end();
    }

    private void showAll() {
        TableElement table = new TableElement();
        table.rightCellPadding(1).rightCellPadding(1);
        table.row(true, "NAME", "DESCRIPTION");
        Map<String, String> map = CommandBuilder.getAllCommandDescription();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }
        session.console(RenderUtil.render(table, session.getCol()));
    }

    private void showCommandHelp(Class<?> definition) {
        Summary summary = definition.getAnnotation(Summary.class);
        if (null != summary) {
            session.console("SUMMARY:\n  " + summary.value());
        }
        Description description = definition.getAnnotation(Description.class);
        if (null != description) {
            session.console(description.value());
        }
    }
}
