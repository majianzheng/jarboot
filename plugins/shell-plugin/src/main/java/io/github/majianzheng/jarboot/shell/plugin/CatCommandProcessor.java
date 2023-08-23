package io.github.majianzheng.jarboot.shell.plugin;

import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Option;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.api.cmd.spi.CommandProcessor;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

/**
 * cat
 * @author jianzhengma
 */
@SuppressWarnings({"squid:S3776", "unused"})
@Name("cat")
@Summary("View the file content")
@Description("Example:\n cat fileName \n cat -n fileName\n cat -n -h[head] 10 fileName\n" +
        " cat -n -t[tail] 10 fileName\n cat -n file -l 10\n cat -n file -l 10:20")
public class CatCommandProcessor implements CommandProcessor {
    private static final int TWO = 2;
    private static final String SPLIT_CHAR = ":";
    private boolean showLine = false;
    private String fileName;
    private Integer head;
    private Integer tail;
    private String lineNumber;
    private volatile boolean isCanceled = false;

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

    @Option(shortName = "l", longName = "line")
    @Description("Read the line: -l10, -l10:20")
    public void setLine(String line) {
        this.lineNumber = line;
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
        Path path = Paths.get(this.fileName);
        File file = path.isAbsolute() ? path.toFile() : new File(UserDirHelper.getCurrentDir(), this.fileName);
        if (file.exists()) {
            this.isCanceled = false;
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
            if (null != this.lineNumber) {
                this.printMiddleFile(session, lr);
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
                if (num > this.head || this.isCanceled) {
                    break;
                }
                session.console(formatLine(line, num));
            }
        } catch (Exception e) {
            session.end(false, e.getMessage());
        }
    }
    private void printMiddleFile(CommandSession session, LineNumberReader lr) throws IOException {
        String[] split = this.lineNumber.split(SPLIT_CHAR);
        if (split.length == 1) {
            int lineNum = Integer.parseInt(this.lineNumber);
            String line;
            while ((line = lr.readLine()) != null) {
                int num = lr.getLineNumber();
                if (num > lineNum || this.isCanceled) {
                    break;
                }
                if (num == lineNum) {
                    //读取指定行
                    session.console(formatLine(line, num));
                }
            }
        } else if (split.length == TWO) {
            int from = Integer.parseInt(split[0]) - 1;
            int to = Integer.parseInt(split[1]);
            String line;
            while ((line = lr.readLine()) != null) {
                int num = lr.getLineNumber();
                if (num > to || this.isCanceled) {
                    break;
                }
                if (num > from) {
                    session.console(formatLine(line, num));
                }
            }
        } else {
            session.end(false, "input error, must one ':'");
        }
    }
    
    private void printTailFile(CommandSession session, LineNumberReader lr) throws IOException {
        if (this.tail > 0) {
            String line;
            LinkedList<String> buffer = new LinkedList<>();
            while ((line = lr.readLine()) != null) {
                if (this.isCanceled) {
                    throw new JarbootRunException("Cat is canceled!");
                }
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
            line = String.format("%6d ┃ %s", num, line);
        }
        return line;
    }

    @Override
    public void onCancel() {
        // 当文件很大时，会造成长时间文件读
        this.isCanceled = true;
    }
}
