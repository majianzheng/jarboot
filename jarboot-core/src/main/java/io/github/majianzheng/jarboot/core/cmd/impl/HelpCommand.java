package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.CommandBuilder;
import io.github.majianzheng.jarboot.text.ui.TableElement;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

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
