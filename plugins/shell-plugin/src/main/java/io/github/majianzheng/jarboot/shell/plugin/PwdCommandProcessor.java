package io.github.majianzheng.jarboot.shell.plugin;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.api.cmd.spi.CommandProcessor;

/**
 * pwd
 * @author jianzhengma
 */
@Name("pwd")
@Summary("show current directory")
@Description(" pwd show current directory")
public class PwdCommandProcessor implements CommandProcessor {
    @Override
    public String process(CommandSession session, String[] args) {
        return UserDirHelper.getCurrentDir();
    }
}
