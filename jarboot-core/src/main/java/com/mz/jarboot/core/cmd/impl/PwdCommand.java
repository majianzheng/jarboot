package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.core.cmd.AbstractCommand;

import java.io.File;

/**
 * @author jianzhengma
 */
@Name("pwd")
@Summary("Return working directory name")
public class PwdCommand extends AbstractCommand {
    @Override
    public void run() {
        String path = new File("").getAbsolutePath();
        session.console(path);
        session.end();
    }
}
