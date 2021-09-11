package com.mz.jarboot.plugin;

import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ls
 * @author jianzhengma
 */
@Name("ls")
@Summary("show the directory files")
@Description(" ls show current directory")
public class LsCommandProcessor implements CommandProcessor {
    private String path;

    @Argument(argName = "path", index = 0, required = false)
    @Description("path")
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        File dir = new File(null == path ? "./" : path);
        if (!dir.isDirectory()) {
            return this.path + " is not a directory.";
        }
        File[] files = dir.listFiles();
        if (null != files && files.length > 0) {
            for (File file : files) {
                String color = file.isDirectory() ? "#3293ed" : "#52c41a";
                final SimpleDateFormat sdf = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");
                String s = sdf.format(new Date(file.lastModified()));
                String text = String.format("<span style=\"color:%s;margin-right:26px\">%s</span>%s",
                        color, file.getName(), s);
                session.console(text);
            }
            return "total: " + files.length;
        } else {
            return this.path + " is empty.";
        }
    }

    @Override
    public void afterProcess(String result, Throwable e) {
        this.path = null;
    }
}
