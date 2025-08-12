package io.github.majianzheng.jarboot.tools.client.command;

import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.text.ui.TableElement;
import io.github.majianzheng.jarboot.text.util.RenderUtil;
import io.github.majianzheng.jarboot.tools.client.ClientCommandBuilder;
import io.github.majianzheng.jarboot.tools.client.InnerConsole;

import java.util.Map;

/**
 * Jarboot client help command
 * @author majianzheng
 */
@Name("help")
@Summary("Display Jarboot client Command Help, help service")
@Description("Examples:\n" + " help\n" + " help list\n" + " help service")
public class ClientHelpCommand extends AbstractClientCommand {
    private String cmd;

    @Argument(index = 0, argName = "cmd", required = false)
    @Description("command name")
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
    @Override
    public void run() {
        if (StringUtils.isEmpty(cmd)) {
            showAll();
            return;
        }
        Class<?> definition = ClientCommandBuilder.getCommandDefineClass(cmd);
        if (null == definition) {
            if (OSUtils.isWindows()) {
                InnerConsole.getInstance().exec("help " + cmd);
            } else {
                InnerConsole.getInstance().exec("man " + cmd);
            }
        } else {
            showCommandHelp(definition);
        }
    }

    @Override
    public void cancel() {
        // default implementation ignored
    }

    private void showAll() {
        TableElement table = new TableElement();
        table.rightCellPadding(1).rightCellPadding(1);
        table.row(true, "NAME", "DESCRIPTION");
        Map<String, String> map = ClientCommandBuilder.getAllCommandDescription();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            table.row(entry.getKey(), entry.getValue());
        }
        println(RenderUtil.render(table, terminal.getWidth()));
    }

    private void showCommandHelp(Class<?> definition) {
        Summary summary = definition.getAnnotation(Summary.class);
        if (null != summary) {
            println("SUMMARY:\n  " + summary.value());
        }
        Description description = definition.getAnnotation(Description.class);
        if (null != description) {
            println(description.value());
        }
    }
}
