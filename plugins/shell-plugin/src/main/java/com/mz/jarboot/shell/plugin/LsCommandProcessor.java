package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ls
 * @author majianzheng
 */
@Name("ls")
@Summary("show the directory files")
@Description(" ls show current directory file list")
public class LsCommandProcessor implements CommandProcessor {
    private String path;

    @Argument(argName = "path", index = 0, required = false)
    @Description("path")
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        String userDir = UserDirUtils.getCurrentDir();
        if (null == this.path) {
            this.path = userDir;
        }
        Path p = Paths.get(this.path);
        File dir = p.isAbsolute() ? p.toFile() : new File(userDir, this.path);
        if (!dir.isDirectory()) {
            return this.path + " is not a directory.";
        }
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
        File[] files = dir.listFiles();
        if (null != files && files.length > 0) {
            for (File file : files) {
                String color = file.isDirectory() ? "#3293ed" : "#52c41a";
                String s = sdf.format(new Date(file.lastModified()));
                String text = String.format("%s<span style=\"color:%s;margin-left:26px\">%s</span>",
                        s, color, file.getName());
                session.console(text);
            }
            return "total: " + files.length;
        } else {
            return this.path + " is empty.";
        }
    }
}
