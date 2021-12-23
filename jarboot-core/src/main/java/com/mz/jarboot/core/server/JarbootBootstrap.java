package com.mz.jarboot.core.server;

import ch.qos.logback.classic.Logger;
import com.alibaba.bytekit.asm.instrument.InstrumentConfig;
import com.alibaba.bytekit.asm.instrument.InstrumentParseResult;
import com.alibaba.bytekit.asm.instrument.InstrumentTransformer;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.IOUtils;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.*;
import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.basic.WsClientFactory;
import com.mz.jarboot.core.stream.StdOutStreamReactor;
import com.mz.jarboot.core.utils.HttpUtils;
import com.mz.jarboot.core.utils.InstrumentationUtils;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.jarboot.SpyAPI;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.CodeSource;
import java.util.*;
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
    private Instrumentation instrumentation;
    private InstrumentTransformer classLoaderInstrumentTransformer;

    private JarbootBootstrap(Instrumentation inst, String args, boolean isPremain) {
        this.instrumentation = inst;
        //1.解析args，获取目标服务端口
        AgentClientPojo clientData = this.initClientData(args, isPremain);
        String sid = clientData.getSid();
        String serverName = clientData.getServer();
        if (EnvironmentContext.isInitialized()) {
            // 第二次进入，检查服务名和sid是否一致
            logger.warn("Jarboot is already initialized. args: {}, isPremain: {}", args, isPremain);
            return;
        } else {
            String jarbootHome = System.getProperty(CommonConst.JARBOOT_HOME);
            //初始化日志模块
            LogUtils.init(jarbootHome, serverName);
            logger = LogUtils.getLogger();

            //2.环境初始化
            EnvironmentContext.init(jarbootHome, clientData, inst);

            //3.initSpy()
            initSpy();

            enhanceClassLoader();
        }

        //4.客户端初始化
        this.initClient();
        if (Boolean.TRUE.equals(clientData.getDiagnose())) {
            WsClientFactory.getInstance().scheduleHeartbeat();
        }

        {
            //fix: attach本地进程时未初始化而不显示控制台输出的问题，初始化标准输出流
            StdOutStreamReactor reactor = StdOutStreamReactor.getInstance();
            if (isPremain) {
                //上线成功开启输出流实时显示
                reactor.setStarting();
            }
        }
    }

    public void initClient() {
        if (WsClientFactory.getInstance().checkOnline()) {
            logger.warn("当前已经处于在线状态，不需要重新连接");
            return;
        }
        EnvironmentContext.cleanSession();

        WsClientFactory.getInstance().createSingletonClient();
    }

    public boolean isOnline(String host) {
        if (EnvironmentContext.isInitialized()) {
            // 第二次进入，检查是否需要变更Jarboot服务地址
            AgentClientPojo clientData = EnvironmentContext.getClientData();
            if (!Objects.equals(host, clientData.getHost())) {
                if (Boolean.TRUE.equals(clientData.getDiagnose())) {
                    AgentClientPojo client = this.initClientData(host, false);
                    clientData.setSid(client.getSid());
                }
                WsClientFactory.getInstance().changeHost(host);
            }
            String sid = clientData.getSid();
            String pid = PidFileHelper.getServerPidString(sid);
            if (!pid.isEmpty() && !PidFileHelper.PID.equals(pid)) {
                logger.warn("pid not match current: {}, pid file: {}", PidFileHelper.PID, pid);
                PidFileHelper.writePidFile(sid);
            }
        } else {
            //以及被执行了shutdown或close命令，此时要重新初始化
            AgentClientPojo client = this.initClientData(host, false);
            //环境重新初始化
            EnvironmentContext.init(null, client, null);
            enhanceClassLoader();
            //连接
            WsClientFactory.getInstance().changeHost(client.getHost());
            //开启输出流
            StdOutStreamReactor.getInstance().enabled(true);
        }
        return WsClientFactory.getInstance().checkOnline();
    }

    public static synchronized JarbootBootstrap getInstance(Instrumentation inst, String args, boolean isPremain) {
        //主入口
        if (bootstrap != null) {
            return bootstrap;
        }
        bootstrap = new JarbootBootstrap(inst, args, isPremain);
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

    private AgentClientPojo initClientData(String args, boolean isPremain) {
        AgentClientPojo clientData = new AgentClientPojo();
        if (isPremain && initPremainArgs(args, clientData)) {
            //由jarboot本地启动时，解析传入参数
            return clientData;
        }
        //args是host的情况
        //设定Host
        String host;
        if (StringUtils.isEmpty(args)) {
            host = System.getProperty(CommonConst.REMOTE_PROP, "127.0.0.1:9899");
        } else {
            host = args;
        }
        if (null == System.getProperty(CommonConst.REMOTE_PROP, null)) {
            System.setProperty(CommonConst.REMOTE_PROP, host);
        }
        String serverName = System.getProperty(CommonConst.SERVER_NAME_PROP, null);
        if (null == serverName) {
            serverName = System.getProperty("sun.java.command", "Name-" + PidFileHelper.PID);
            serverName = serverName.split(StringUtils.SPACE, 2)[0];
        }

        StringBuilder sb = new StringBuilder();
        sb
                .append(PidFileHelper.PID)
                .append(CommonConst.COMMA_SPLIT)
                .append(PidFileHelper.INSTANCE_NAME)
                .append(CommonConst.COMMA_SPLIT)
                .append(serverName);

        String url = String.format("http://%s/api/jarboot/public/agent/agentClient", host);
        clientData = HttpUtils.postJson(url, sb.toString(), AgentClientPojo.class);
        if (null == clientData) {
            throw new JarbootException("Request Jarboot server failed! url:" + url);
        }
        if (0 != clientData.getResultCode()) {
            throw new JarbootException(clientData.getResultCode(), clientData.getResultMsg());
        }
        clientData.setHost(host);
        return clientData;
    }

    private boolean initPremainArgs(String args, AgentClientPojo clientData) {
        if (StringUtils.isBlank(args)) {
            return false;
        }
        String s = new String(Base64.getDecoder().decode(args));
        String[] agentArgs = s.split(String.valueOf(CommandConst.PROTOCOL_SPLIT));
        if (agentArgs.length != 3) {
            throw new JarbootException("解析传入传入参数错误！args:" + s);
        }
        clientData.setHost(String.format("127.0.0.1:%s", agentArgs[0]));
        System.setProperty(CommonConst.REMOTE_PROP, clientData.getHost());
        clientData.setServer(agentArgs[1]);
        String sid = agentArgs[2];
        clientData.setSid(sid);
        PidFileHelper.writePidFile(sid);
        return true;
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
}
