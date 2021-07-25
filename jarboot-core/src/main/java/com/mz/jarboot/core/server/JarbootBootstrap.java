package com.mz.jarboot.core.server;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.classic.Logger;
import com.alibaba.bytekit.asm.instrument.InstrumentConfig;
import com.alibaba.bytekit.asm.instrument.InstrumentParseResult;
import com.alibaba.bytekit.asm.instrument.InstrumentTransformer;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.common.JsonUtils;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.basic.WsClientFactory;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.cmd.CommandDispatcher;
import com.mz.jarboot.core.utils.InstrumentationUtils;
import com.mz.jarboot.core.ws.MessageHandler;
import com.mz.jarboot.core.ws.WebSocketClient;
import io.netty.channel.Channel;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.jarboot.SpyAPI;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.CodeSource;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * attach 启动入口，为减轻对程序对侵入，将程序作为客户端，由服务端反向连接
 * @author majianzheng
 */
@SuppressWarnings("all")
public class JarbootBootstrap {
    private static final String SPY_JAR = "jarboot-spy.jar";
    private static Logger logger;
    private static JarbootBootstrap bootstrap;
    private CommandDispatcher dispatcher;
    private String host;
    private String serverName;
    private WebSocketClient client;
    private Instrumentation instrumentation;
    private InstrumentTransformer classLoaderInstrumentTransformer;
    private volatile boolean online = false;
    private MessageHandler messageHandler;

    private JarbootBootstrap(Instrumentation inst, String args) {
        if (null == args || args.isEmpty()) {
            return;
        }
        this.instrumentation = inst;

        //1.解析args，获取目标服务端口
        String s = new String(Base64.getDecoder().decode(args));
        JsonNode json = JsonUtils.readAsJsonNode(s);
        host = json.get("host").asText();
        serverName = json.get("server").asText();

        //2.环境初始化
        EnvironmentContext.init(serverName, host, inst);
        initLogback(); //初始化日志模块
        logger.info("获取参数>>>{}, server:{}, args:{}", host, serverName, args);

        //3.initSpy()
        initSpy();

        enhanceClassLoader();
        //4.命令派发器
        dispatcher = new CommandDispatcher();

        //5.初始化WebSocket的handler
        this.initMessageHandler();

        //6.客户端初始化
        this.initClient();
    }
    private void initMessageHandler() {
        this.messageHandler = new MessageHandler() {
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
        };
    }
    public void initClient() {
        if (online) {
            logger.warn("当前已经处于在线状态，不需要重新连接");
            return;
        }

        if (null != client && !client.isOpen()) {
            //已经不在线，清理资源重新连接
            logger.info("已离线，正在重新初始化客户端...");
            client.disconnect();
        }

        EnvironmentContext.cleanSession();

        client = WsClientFactory.getInstance().createSingletonClient(this.messageHandler);
        if (null == client) {
            online = false;
            logger.error("连接失败！");
            return;
        }
        if (client.isOpen()) {
            online = true;
        } else {
            logger.info("尝试重新连接中..");
            if (client.connect(this.messageHandler)) {
                online = true;
                logger.info("尝试重新连接成功！");
            } else {
                online = false;
                logger.error("尝试重新连接失败！");
            }
        }
    }

    public boolean isOnline() {
        return online;
    }

    public static synchronized JarbootBootstrap getInstance(Instrumentation inst, String args) {
        //主入口
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

    private void initSpy() {
        // 将Spy添加到BootstrapClassLoader
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        Class<?> cls = null;
        if (parent != null) {
            try {
                cls = parent.loadClass("java.jarboot.SpyAPI");
            } catch (Throwable e) {
                // ignore
            }
        }
        if (null == cls) {
            try {
                CodeSource codeSource = JarbootBootstrap.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    File coreJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                    File spyJarFile = new File(coreJarFile.getParentFile(), SPY_JAR);
                    instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(spyJarFile));
                } else {
                    logger.error("can not find {}", SPY_JAR);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }

        //初始化
        try {
            SpyAPI.init();
        } catch (Exception e) {
            // ignore
        }
    }

    void enhanceClassLoader() {
        Set<String> loaders = new HashSet<String>();
        // 增强 ClassLoader#loadClsss ，解决一些ClassLoader加载不到 SpyAPI的问题
        byte[] classBytes = new byte[0];
        try {
            classBytes = IOUtils.getBytes(JarbootBootstrap.class.getClassLoader()
                    .getResourceAsStream(ClassLoader_Instrument.class.getName().replace('.', '/') + ".class"));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        SimpleClassMatcher matcher = new SimpleClassMatcher(loaders);
        InstrumentConfig instrumentConfig = new InstrumentConfig(AsmUtils.toClassNode(classBytes), matcher);

        InstrumentParseResult instrumentParseResult = new InstrumentParseResult();
        instrumentParseResult.addInstrumentConfig(instrumentConfig);
        classLoaderInstrumentTransformer = new InstrumentTransformer(instrumentParseResult);
        instrumentation.addTransformer(classLoaderInstrumentTransformer, true);

        if (loaders.size() == 1 && loaders.contains(ClassLoader.class.getName())) {
            // 如果只增强 java.lang.ClassLoader，可以减少查找过程
            try {
                instrumentation.retransformClasses(ClassLoader.class);
            } catch (UnmodifiableClassException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            InstrumentationUtils.trigerRetransformClasses(instrumentation, loaders);
        }
    }

    /**
     * 自定义日志，防止与目标进程记录到同一文件中
     */
    private static void initLogback() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] " +
                "[%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        StringBuilder sb = new StringBuilder();
        sb
                .append(EnvironmentContext.getJarbootHome())
                .append(File.separator)
                .append("logs")
                .append(File.separator)
                .append(EnvironmentContext.getServer())
                .append(File.separator)
                .append("jarboot-")
                .append(EnvironmentContext.getServer())
                .append(".log");
        String log =sb.toString();
        fileAppender.setFile(log);
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        //模块中所有日志均使用该名字获取
        logger = (Logger) LoggerFactory.getLogger(CoreConstant.LOG_NAME);
        logger.addAppender(fileAppender);
    }
}
