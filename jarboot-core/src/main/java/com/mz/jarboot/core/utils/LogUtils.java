package com.mz.jarboot.core.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.core.constant.CoreConstant;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author majianzheng
 */
public class LogUtils {
    private static Logger logger;
    private static String logDir;

    public static void init(String home, String server, String sid) {
        if (null != logger) {
            return;
        }
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] " +
                "[%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        StringBuilder sb = new StringBuilder();
        sb
                .append(home)
                .append(File.separator)
                .append("logs")
                .append(File.separator)
                .append(server);
        logDir = sb.toString();
        sb.append(File.separator)
                .append("jarboot-")
                .append(server)
                .append('-')
                .append(sid)
                .append(".log");
        String log =sb.toString();
        fileAppender.setFile(log);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        //模块中所有日志均使用该名字获取
        logger = (Logger) LoggerFactory.getLogger(CoreConstant.LOG_NAME);
        logger.addAppender(fileAppender);
        writePidFile(sid);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getLogDir() {
        return logDir;
    }

    public static void writePidFile(String sid) {
        //写入pid
        File logsDir = FileUtils.getFile(logDir);
        File pid = FileUtils.getFile(logsDir, sid + CommonConst.PID_EXT);
        try {
            if (!logsDir.exists()) {
                FileUtils.forceMkdir(logsDir);
            }
            String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            int index = name.indexOf('@');
            FileUtils.writeStringToFile(pid, name.substring(0, index), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void deletePidFile(String sid) {
        File pid = FileUtils.getFile(logDir, sid + CommonConst.PID_EXT);
        try {
            FileUtils.forceDelete(pid);
        } catch (Exception exception) {
            //ignore
        }
    }

    private LogUtils() {}
}
