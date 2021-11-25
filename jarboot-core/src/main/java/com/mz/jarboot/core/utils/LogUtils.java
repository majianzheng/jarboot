package com.mz.jarboot.core.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author majianzheng
 */
public class LogUtils {
    private static Logger logger;
    private static String logDir;

    public static void init(String home, String server, String sid, boolean persist) {
        if (null != logger) {
            return;
        }
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] " +
                "[%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        StringBuilder sb = new StringBuilder();
        sb
                .append(home)
                .append(File.separator)
                .append("logs")
                .append(File.separator)
                .append(server);
        logDir = sb.toString();
        Appender<ILoggingEvent> appender;
        if (persist) {
            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
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
            appender = fileAppender;
        } else {
            ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
            consoleAppender.setEncoder(ple);
            consoleAppender.setContext(lc);
            consoleAppender.start();
            appender = consoleAppender;
        }

        //模块中所有日志均使用该名字获取
        logger = (Logger) LoggerFactory.getLogger(CoreConstant.LOG_NAME);
        logger.addAppender(appender);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getLogDir() {
        return logDir;
    }

    private LogUtils() {}
}
