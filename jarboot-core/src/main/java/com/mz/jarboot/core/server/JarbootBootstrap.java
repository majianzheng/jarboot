package com.mz.jarboot.core.server;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.classic.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.core.advisor.TransformerManager;
import com.mz.jarboot.core.constant.JarbootCoreConstant;
import com.mz.jarboot.core.msg.CommandHandler;
import com.mz.jarboot.core.msg.ResponseBuilder;
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
    private TransformerManager transformerManager;
    private WebSocketClient client;
    private CommandHandler handler;
    private String host;
    private String serverName;
    private boolean online = false;

    private JarbootBootstrap(Instrumentation inst, String args) {
        transformerManager = new TransformerManager(inst);
        if (null == args || args.isEmpty()) {
            return;
        }
        //解析args，获取目标服务端口
        String s = new String(Base64.getDecoder().decode(args));
        JSONObject json = JSON.parseObject(s);
        host = json.getString("host");
        serverName = json.getString("server");
        logger.info("获取参数>>>" + host + ", server: " + serverName);
        this.initClient();
    }
    private void initClient() {
        String url = String.format("ws://%s/jarboot-agent/ws", host);
        logger.debug("initClient>>>" + url);
        client = new WebSocketClient(url);
        handler = new CommandHandler(host);
        boolean isOk = client.connect(new MessageHandler() {
            @Override
            public void onOpen(Channel channel) {
                online = true;
                logger.debug("连接成功>>>");
            }

            @Override
            public void onText(String text, Channel channel) {
                handler.onMsgRecv(text);
            }

            @Override
            public void onBinary(byte[] bytes, Channel channel) {
                handler.onMsgRecv(new String(bytes));
            }

            @Override
            public void onClose(Channel channel) {
                if (online) {
                    online = false;
                    //启动尝试重连机制
                }
                logger.debug("连接关闭>>>");
            }

            @Override
            public void onError(Channel channel) {
                logger.error("连接异常>>>");
                onClose(channel);
            }
        });
        logger.debug("连接结果>>>" + isOk);
        client.sendText(genOnlineResponse(serverName));
    }

    private String genOnlineResponse(String body) {
        CommandResponse resp = new ResponseBuilder().setType(CommandConst.ONLINE_TYPE
        ).setBody(body).getResponse();
        return JSON.toJSONString(resp);
    }

    public boolean isOnline() {
        return online;
    }

    public synchronized static JarbootBootstrap getInstance(Instrumentation inst, String args) {
        //主入口
        initLogback();

        logger.debug("getInstance>>>" + args);
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
        logger = (Logger) LoggerFactory.getLogger(JarbootCoreConstant.LOG_NAME);
        logger.addAppender(fileAppender);
    }
}
