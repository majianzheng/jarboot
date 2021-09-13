package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import java.io.File;

/**
 * cd
 * @author majianzheng
 */
@Name("cd")
@Summary("Change current work directory")
@Description("Same as sysprop user.dir xxx\n cd directory\n cd ../../")
public class CdCommandProcessor implements CommandProcessor {
    private String path;

    @Argument(argName = "path", index = 0)
    @Description("path")
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        File dir = new File(path);
        if (!dir.isDirectory() || !dir.exists()) {
            return this.path + " is not a directory.";
        }
        System.setProperty("user.dir", dir.getAbsolutePath());
        return "";
    }

    @Override
    public void afterProcess(String result, Throwable e) {
        this.path = null;
    }
}
