package com.mz.jarboot.terminal;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.JarbootThreadFactory;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.ws.SessionOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 控制台终端
 * @author mazheng
 */
public class TerminalProcess {
    private static final Logger logger = LoggerFactory.getLogger(TerminalProcess.class);
    private Process process;
    private Thread readerThread;
    private OutputStream outputStream;
    private SessionOperator operator;
    public void init(Session session) {
        operator = new SessionOperator(session);
        try {
            process = Runtime.getRuntime().exec(new String[]{"bash", "-i"});
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
        if (!cmd.endsWith(StringUtils.LF)) {
            cmd += StringUtils.LF;
        }
        try {
            outputStream.write(cmd.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    public void destroy() {
        process.destroy();
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        readerThread = null;
    }

    private void run() {
        try (InputStream inputStream = process.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            while (process.isAlive()) {
                sendMsg(reader);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendMsg(BufferedReader reader) {
        try {
            operator.newMessage(reader.readLine());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
