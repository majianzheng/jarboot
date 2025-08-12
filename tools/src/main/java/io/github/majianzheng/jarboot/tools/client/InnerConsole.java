package io.github.majianzheng.jarboot.tools.client;

import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.jline.reader.LineReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 控制台终端
 * @author mazheng
 */
@SuppressWarnings({"java:S106", "java:S2274", "java:S3776", "java:S1170"})
public class InnerConsole {
    private static final InnerConsole CONSOLE = new InnerConsole();
    private Process process;
    private Thread readerThread;
    private Thread errorThread;
    private OutputStream outputStream;
    private volatile boolean running;
    private LineReader lineReader;
    private volatile boolean getWorkDirRunning;
    private String workDir;
    private final String winEndFlag = ">";

    public static InnerConsole getInstance() {
        return CONSOLE;
    }

    public synchronized void init(LineReader lineReader) {
        this.lineReader = lineReader;
        try {
            String[] cmd = { "/bin/bash", "-l" };
            if (OSUtils.isWindows()) {
                cmd = new String[]{"cmd"};
            }
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm");
            process = new ProcessBuilder().command(cmd).start();

            outputStream = process.getOutputStream();
            readerThread = JarbootThreadFactory
                    .createThreadFactory("terminal-", true)
                    .newThread(this::run);
            readerThread.start();
            errorThread = JarbootThreadFactory
                    .createThreadFactory("terminal-error-", true)
                    .newThread(this::runError);
            errorThread.start();
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public String getWordDir() {
        getWorkDirRunning = true;
        workDir = "";
        String cmd = OSUtils.isWindows() ? "cd" : "pwd";
        exec(cmd);
        if (OSUtils.isWindows() && workDir.endsWith(winEndFlag)) {
            workDir = workDir.substring(0, workDir.length() - 1);
        }
        return workDir;
    }

    public synchronized void exec(String cmd) {
        if (!process.isAlive()) {
            return;
        }
        if (!cmd.endsWith(StringUtils.LF)) {
            cmd += StringUtils.LF;
        }
        if (running) {
            final Character mask = OSUtils.isWindows() ? (char)0 : null;
            String input = lineReader.readLine("Command is running cancel? Y/N: ", mask);
            final String y = "Y";
            final String yes = "yes";
            if (input.equalsIgnoreCase(y) || input.equalsIgnoreCase(yes)) {
                destroy();
                init(lineReader);
            } else {
                return;
            }
        }
        try {
            running = true;
            outputStream.write(cmd.getBytes());
            if (!OSUtils.isWindows()) {
                outputStream.write("echo \"jarboot$>\"\n".getBytes());
            }
            outputStream.flush();
            this.wait(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e.getMessage(), e);
        }
    }

    public synchronized void destroy() {
        process.destroyForcibly();
        try {
            process.waitFor();
            if (null != readerThread) {
                readerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            AnsiLog.info("销毁终端");
        }
        readerThread = null;
        errorThread = null;
    }

    private void runError() {
        try (InputStream errorStream = process.getErrorStream()) {
            int i = 0;
            byte[] buffer = new byte[2048];
            while ((i = errorStream.read(buffer)) != -1) {
                String msg;
                if (OSUtils.isWindows()) {
                    msg = new String(buffer, 0, i, "GBK");
                } else {
                    msg = new String(buffer, 0, i);
                }
                System.err.print(msg);
            }
        } catch (Exception e) {
            AnsiLog.error(e.getMessage(), e);
        }
    }
    private void run() {
        try (InputStream inputStream = process.getInputStream()) {
            int i = 0;
            byte[] buffer = new byte[2048];
            while ((i = inputStream.read(buffer)) != -1) {
                String msg;
                if (OSUtils.isWindows()) {
                    msg = new String(buffer, 0, i, "GBK");
                    if (msg.endsWith(winEndFlag)) {
                        System.out.println(msg);
                        synchronized (this) {
                            running = false;
                            this.notifyAll();
                        }
                    } else {
                        System.out.print(msg);
                    }
                } else {
                    msg = new String(buffer, 0, i);
                    int index = msg.indexOf("jarboot$>");
                    if (index >= 0) {
                        msg = msg.substring(0, index);
                        if (index > 0) {
                            System.out.print(msg);
                        }
                        synchronized (this) {
                            running = false;
                            this.notifyAll();
                        }
                    } else {
                        System.out.print(msg);
                    }
                }
                if (getWorkDirRunning && !running) {
                    workDir = msg.trim();
                    getWorkDirRunning = false;
                }
            }
        } catch (Exception e) {
            AnsiLog.error(e.getMessage(), e);
        }
    }
}
