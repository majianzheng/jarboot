package com.mz.jarboot.shell.plugin;

import com.mz.jarboot.api.cmd.annotation.Argument;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Option;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;

/**
 * cat
 * @author jianzhengma
 */
@Name("cat")
@Summary("View the file content")
@Description("Example:\n cat fileName \n cat -n fileName\n cat -n -h[head] 10 fileName\n cat -n -t[tail] 10 fileName")
public class CatCommandProcessor implements CommandProcessor {
    private boolean showLine = false;
    private String fileName;
    private Integer head;
    private Integer tail;

    @Option(shortName = "n", longName = "number", flag = true)
    @Description("Show line number or not")
    public void setNumber(boolean n) {
        this.showLine = n;
    }
    
    @Option(shortName = "h", longName = "head")
    @Description("Show lines top")
    public void setHead(int n) {
        this.head = n;
    }
    
    @Option(shortName = "t", longName = "tail")
    @Description("Show lines tail")
    public void setTail(int n) {
        this.tail = n;
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
        if (null != this.head && null != this.tail) {
            session.end(false, "head and tail option only support one!");
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
        try (FileReader reader = new FileReader(file);
                LineNumberReader lr = new LineNumberReader(reader)) {
            if (null != this.tail) {
                printTailFile(session, lr);
                return;
            }
            if (null == this.head) {
                this.head = Integer.MAX_VALUE - 1;
            } else {
                if (this.head <= 0) {
                    session.end(false, "top line must big than 0");
                    return;
                }
            }
            String line;
            while ((line = lr.readLine()) != null) {
                int num = lr.getLineNumber();
                if (num > this.head) {
                    break;
                }
                session.console(formatLine(line, num));
            }
        } catch (Exception e) {
            session.end(false, e.getMessage());
        }
    }
    
    private void printTailFile(CommandSession session, LineNumberReader lr) throws IOException {
        if (this.tail > 0) {
            String line;
            LinkedList<String> buffer = new LinkedList<>();
            while ((line = lr.readLine()) != null) {
                buffer.add(formatLine(line, lr.getLineNumber()));
                if (buffer.size() > this.tail) {
                    buffer.removeFirst();
                }
            }
            buffer.forEach(session::console);
        } else {
            session.end(false, "tail line must big than 0");
        }
    }
    
    private String formatLine(String line, int num) {
        if (this.showLine) {
            line = String.format("<span style=\"color:gray;margin:0 20px 0 2px\">%d</span>%s", num, line);
        }
        return line;
    }

    @Override
    public void afterProcess(String result, Throwable e) {
        this.showLine = false;
        this.fileName = null;
        this.tail = null;
        this.head = null;
    }
}
