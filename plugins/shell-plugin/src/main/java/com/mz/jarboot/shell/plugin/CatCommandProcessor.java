package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Option;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * cat
 * @author jianzhengma
 */
@Name("cat")
@Summary("view the file content")
@Description(" cat fileName \n cat -n fileName")
public class CatCommandProcessor implements CommandProcessor {
    private boolean showLine = false;
    private String fileName;

    @Option(shortName = "n", longName = "number", flag = true)
    @Description("Show line number or not")
    public void setNumber(boolean n) {
        this.showLine = n;
    }
    
    @Argument(argName = "file", index = 0)
    @Description("file name")
    public void setFile(String file) {
        this.fileName = file;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        if (null == this.fileName || this.fileName.isEmpty()) {
            session.end(false, "file name is empty!");
            return "";
        }
        File file = new File(this.fileName);
        if (file.exists()) {
            this.printFile(file, session);
        } else {
            session.end(false, this.fileName + " is not exists!");
        }
        return "";
    }
    
    private void printFile(File file, CommandSession session) {
        int number = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (this.showLine) {
                    line = String.format("<span style=\"color:gray;margin:0 20px 0 2px\">%d</span>%s", ++number, line);
                }
                session.console(line);
            }
        } catch (Exception e) {
            session.end(false, e.getMessage());
        }
    }

    @Override
    public void afterProcess(String result, Throwable e) {
        this.showLine = false;
        this.fileName = null;
    }
}
