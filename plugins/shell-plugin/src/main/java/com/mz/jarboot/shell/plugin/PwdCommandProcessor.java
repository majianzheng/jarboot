package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.cmd.annotation.*;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;

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
