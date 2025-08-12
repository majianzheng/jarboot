package io.github.majianzheng.jarboot.terminal;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.utils.BannerUtils;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.ws.SessionOperator;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.*;
import java.util.HashMap;
import java.util.List;
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
    public synchronized void init(Session session) {
        operator = new SessionOperator(session);
        int col = 80;
        int row = 100;
        String colParam = getRequestParam(session, "col");
        String rowParam = getRequestParam(session, "row");
        if (!colParam.isEmpty()) {
            col = Integer.parseInt(colParam);
        }
        if (!rowParam.isEmpty()) {
            row = Integer.parseInt(rowParam);
        }
        operator.newMessage(BannerUtils.colorBanner());
        try {
            String[] cmd = { "/bin/bash", "-l" };
            if (OSUtils.isWindows()) {
                cmd = new String[]{"cmd"};
            }
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "xterm");
            String directory = getWorkDirectory(session);
            process = new PtyProcessBuilder()
                    .setCommand(cmd)
                    .setDirectory(directory)
                    .setWindowsAnsiColorEnabled(true)
                    .setInitialColumns(col)
                    .setInitialRows(row)
                    .setEnvironment(env)
                    .start();

            outputStream = process.getOutputStream();
            readerThread = JarbootThreadFactory
                    .createThreadFactory("terminal-", true)
                    .newThread(this::run);
            readerThread.start();
            logger.info("启动终端session:{}，宽：{}, 高：{}, 工作目录: {}", session.getId(), col, row, directory);
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public void exec(String cmd) {
        if (!process.isAlive()) {
            logger.error("终端进程已停止！");
            return;
        }
        try {
            outputStream.write(cmd.getBytes());
            outputStream.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void setWinSize(int columns, int rows) {
        try {
            process.setWinSize(new WinSize(columns, rows));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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
            logger.info("销毁终端");
        }
        readerThread = null;
    }

    private String getWorkDirectory(Session session) {
        String userDir = getRequestParam(session, CommonConst.USER_DIR);
        File dir = StringUtils.isEmpty(userDir) ? FileUtils.getFile(SettingUtils.getWorkspace()) : FileUtils.getFile(SettingUtils.getWorkspace(), userDir);
        if (dir.isDirectory() && dir.exists()) {
            return dir.getAbsolutePath();
        }
        dir = FileUtils.getFile(SettingUtils.getWorkspace());
        return dir.getAbsolutePath();
    }

    private String getRequestParam(Session session, String key) {
        List<String> colParam = session.getRequestParameterMap().get(key);
        if (null != colParam && !colParam.isEmpty()) {
            return colParam.get(0);
        }
        return StringUtils.EMPTY;
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
        } finally {
            operator.newMessage("Terminal process exit.");
            operator.close();
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
