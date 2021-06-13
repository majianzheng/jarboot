package com.mz.jarboot.core.server;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.mz.jarboot.core.constant.CoreConstant;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class LogTest {
    private static Logger logger;
    public static void initTest() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setEncoder(ple);
        consoleAppender.setContext(lc);
        consoleAppender.start();

        //模块中所有日志均使用该名字获取
        logger = (Logger) LoggerFactory.getLogger(CoreConstant.LOG_NAME);
        logger.addAppender(consoleAppender);
    }
    @BeforeClass
    public static void init() {
        LogTest.initTest();
    }
    @Test
    public void test() {
        logger.info("test info");
        org.junit.Assert.assertTrue(true);
    }
}
