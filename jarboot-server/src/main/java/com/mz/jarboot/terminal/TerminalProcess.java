package com.mz.jarboot.terminal;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.JarbootThreadFactory;
import com.mz.jarboot.common.utils.BannerUtils;
import com.mz.jarboot.ws.SessionOperator;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 控制台终端
 * @author mazheng
 */
public class TerminalProcess {
    private static final Logger logger = LoggerFactory.getLogger(TerminalProcess.class);
    private PtyProcess process;
    private Thread readerThread;
    private OutputStream outputStream;
    private SessionOperator operator;
    public void init(Session session) {
        operator = new SessionOperator(session);
        operator.newMessage(BannerUtils.colorBanner());
        try {
            String[] cmd = { "/bin/sh", "-l" };
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm");
            process = new PtyProcessBuilder().setCommand(cmd).setWindowsAnsiColorEnabled(true).setEnvironment(env).start();

            outputStream = process.getOutputStream();
            readerThread = JarbootThreadFactory
                    .createThreadFactory("terminal-", true)
                    .newThread(this::run);
            readerThread.start();
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    public void exec(String cmd) {
        if (!process.isAlive()) {
            logger.error("终端进程已停止！");
            return;
        }
        try {
            outputStream.write(cmd.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    public void setWinSize(int columns, int rows) {
        process.setWinSize(new WinSize(columns, rows));
    }

    public void destroy() {
        process.destroyForcibly();
        try {
            process.waitFor();
            readerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        readerThread = null;
    }

    private void run() {
        try (InputStream inputStream = process.getInputStream()) {
            int i = 0;
            byte[] buffer = new byte[2048];
            while ((i = inputStream.read(buffer)) != -1) {
                String msg = new String(buffer, 0, i);
                sendMsg(msg);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendMsg(String str) {
        try {
            operator.newMessage(str);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
