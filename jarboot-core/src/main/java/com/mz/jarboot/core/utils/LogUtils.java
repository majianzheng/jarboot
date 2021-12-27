package com.mz.jarboot.core.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author majianzheng
 */
public class LogUtils {
    private static Logger logger;
    private static String logDir;

    public static void init(String home, String server) {
        if (null != logger) {
            return;
        }
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

    private LogUtils() {}
}
