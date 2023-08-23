package io.github.majianzheng.jarboot.core.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import io.github.majianzheng.jarboot.common.AnsiLog;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author majianzheng
 */
public class LogUtils {
    /** 未在线时记录本地日志文件 */
    public static final String LOCAL_FILE_NAME = "jarboot-core.log";
    /** 日志文件最大的大小 10M，超过时清空重写 */
    public static final long MAX_FILE_SIZE = (1024 * 1024 * 10);
    private static Logger logger;
    private static String logDir;
    private static String server;
    private static boolean dev = false;

    public static void init(String home, String server) {
        if (null != logger) {
            return;
        }
        dev = Objects.equals("true",  System.getProperty("jarboot.dev"));
        LogUtils.server = server;
        logDir = home + File.separator + "logs";
        //模块中所有日志均使用该名字获取
        logger = (Logger) LoggerFactory.getLogger("ROOT");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        String serverName = String.format("[%s] ", server);
        encoder.setPattern("%date  %level " + serverName + "[%thread] [%file:%line] %msg%n");
        encoder.setContext(lc);
        encoder.start();
        StreamCrossLogAppender<ILoggingEvent> appender = new StreamCrossLogAppender<>();
        appender.setEncoder(encoder);
        appender.setContext(lc);
        appender.start();
        logger.addAppender(appender);
        //Console日志语法高亮
        ch.qos.logback.core.ConsoleAppender<ILoggingEvent> console =
                (ch.qos.logback.core.ConsoleAppender<ILoggingEvent>)logger.getAppender("console");
        if (null != console) {
            console.stop();
            PatternLayoutEncoder colorEncoder = new PatternLayoutEncoder();
            colorEncoder.setPattern("%date %highlight(%-5level) [%thread] [%cyan(%file:%line)] %msg%n");
            colorEncoder.setContext(lc);
            colorEncoder.start();
            console.setEncoder(colorEncoder);
            console.start();
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getLogDir() {
        return logDir;
    }

    public static void offlineDevLog(String msg, Object... arguments) {
       if (dev) {
           StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
           StackTraceElement callingFrame = stackTraceElements[2];
           String prefix = String.format("[ offline ] [%s] %s %s [%d] ", server, callingFrame.getMethodName(), callingFrame.getFileName(), callingFrame.getLineNumber());
           String content = prefix + AnsiLog.format(msg, arguments) + "\n";
           writeLocalLog(content.getBytes(StandardCharsets.UTF_8));
       }
    }

    public static void writeLocalLog(byte[] byteArray) {
        final java.io.File logFile = getLocalLogFile();
        try {
            if (!logFile.exists() && !createLogFile(logFile)) {
                return;
            }
            FileUtils.writeByteArrayToFile(logFile, byteArray, logFile.length() < LogUtils.MAX_FILE_SIZE);
        } catch (Exception e) {
            //ignore
        }
    }

    private static boolean createLogFile(java.io.File logFile) throws IOException {
        java.io.File logDir = logFile.getParentFile();
        if (!logDir.exists() && !logDir.mkdirs()) {
            return false;
        }
        return logFile.createNewFile();
    }

    private static java.io.File getLocalLogFile() {
        return FileUtils.getFile(LogUtils.getLogDir(), LOCAL_FILE_NAME);
    }

    private LogUtils() {}
}
