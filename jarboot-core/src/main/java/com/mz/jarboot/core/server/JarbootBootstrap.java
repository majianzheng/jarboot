package com.mz.jarboot.core.server;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.classic.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.basic.SingletonCoreFactory;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.cmd.CommandDispatcher;
import com.mz.jarboot.core.ws.MessageHandler;
import com.mz.jarboot.core.ws.WebSocketClient;
import io.netty.channel.Channel;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Base64;

public class JarbootBootstrap {
    private static Logger logger;
    private static JarbootBootstrap bootstrap;
    private CommandDispatcher dispatcher;
    private String host;
    private String serverName;
    private WebSocketClient client;
    private boolean online = false;

    private JarbootBootstrap(Instrumentation inst, String args) {
        if (null == args || args.isEmpty()) {
            return;
        }
        //解析args，获取目标服务端口
        String s = new String(Base64.getDecoder().decode(args));
        JSONObject json = JSON.parseObject(s);
        host = json.getString("host");
        serverName = json.getString("server");
        logger.info("获取参数>>>{}, server:{}", host, serverName);
        EnvironmentContext.init(serverName, host, new TransformerManager(inst));
        dispatcher = new CommandDispatcher();
        this.initClient();
    }
    public void initClient() {
        if (online) {
            logger.warn("当前已经处于在线状态，不需要重新连接");
            return;
        }

        if (null != client) {
            //已经不在线，清理资源重新连接
            logger.info("已离线，正在重新初始化客户端...");
            client.disconnect();
        }
        logger.info("创建客户端实例》》》》》》");
        EnvironmentContext.cleanSession();
        client = SingletonCoreFactory.getInstance().createSingletonClient(new MessageHandler() {
            @Override
            public void onOpen(Channel channel) {
                logger.debug("连接成功>>>");
            }

            @Override
            public void onText(String text, Channel channel) {
                dispatcher.execute(text);
            }

            @Override
            public void onBinary(byte[] bytes, Channel channel) {
                dispatcher.execute(new String(bytes));
            }

            @Override
            public void onClose(Channel channel) {
                if (online) {
                    online = false;
                }
                EnvironmentContext.cleanSession();
                logger.debug("连接关闭>>>");
            }

            @Override
            public void onError(Channel channel) {
                logger.error("连接异常>>>");
                onClose(channel);
            }
        });
        logger.info("initClient {}", client);
        if (null != client) {
            online = true;
            logger.info("上线成功！");
        }
    }

    public boolean isOnline() {
        return online;
    }

    public static synchronized JarbootBootstrap getInstance(Instrumentation inst, String args) {
        //主入口
        initLogback();

        logger.debug("getInstance{}", args);
        if (bootstrap != null) {
            return bootstrap;
        }
        bootstrap = new JarbootBootstrap(inst, args);
        return bootstrap;
    }
    public static JarbootBootstrap getInstance() {
        if (null == bootstrap) {
            throw new IllegalStateException("Jarboot must be initialized before!");
        }
        return bootstrap;
    }

    /**
     * 自定义日志，防止与目标进程记录到同一文件中
     */
    private static void initLogback() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        String log = System.getProperty("user.home") + File.separator + "jarboot" + File.separator + "logs" +
                File.separator + "core.log";
        fileAppender.setFile(log);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        //模块中所有日志均使用该名字获取
        logger = (Logger) LoggerFactory.getLogger(CoreConstant.LOG_NAME);
        logger.addAppender(fileAppender);
    }
}
