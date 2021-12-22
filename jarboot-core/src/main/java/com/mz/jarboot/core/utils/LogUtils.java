package com.mz.jarboot.core.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mz.jarboot.core.constant.CoreConstant;
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
        logger = (Logger) LoggerFactory.getLogger(CoreConstant.LOG_NAME);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("[" + server + "] %date %level [%thread] " +
                "[%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        StreamCrossLogAppender<ILoggingEvent> appender = new StreamCrossLogAppender<>();
        appender.setEncoder(ple);
        appender.setContext(lc);
        appender.setName(CoreConstant.LOG_NAME);
        appender.start();
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
