package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        Path p = Paths.get(this.path);
        File dir = p.isAbsolute() ? p.toFile() : new File(UserDirHelper.getCurrentDir(), this.path);
        if (!dir.isDirectory() || !dir.exists()) {
            return this.path + " is not a directory.";
        }
        try {
            UserDirHelper.setCurrentDir(dir.getCanonicalPath());
        } catch (IOException e) {
            session.end(false, e.getMessage());
        }
        return "";
    }
}
