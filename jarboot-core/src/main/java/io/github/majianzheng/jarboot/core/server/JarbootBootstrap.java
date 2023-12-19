package io.github.majianzheng.jarboot.core.server;

import com.alibaba.bytekit.asm.instrument.InstrumentConfig;
import com.alibaba.bytekit.asm.instrument.InstrumentParseResult;
import com.alibaba.bytekit.asm.instrument.InstrumentTransformer;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.IOUtils;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.pojo.AgentClient;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.basic.WsClientFactory;
import io.github.majianzheng.jarboot.core.stream.StdOutStreamReactor;
import io.github.majianzheng.jarboot.core.stream.ResultStreamDistributor;
import io.github.majianzheng.jarboot.core.utils.InstrumentationUtils;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
@SuppressWarnings({"unused", "squid:S1181"})
public class JarbootBootstrap {
    private static final String SPY_JAR = "jarboot-spy.jar";
    private static JarbootBootstrap bootstrap;
    private Instrumentation instrumentation;

    private JarbootBootstrap(Instrumentation inst, String args, boolean isPremain) {
        this.instrumentation = inst;
        // 初始化事件触发器
        ResultStreamDistributor.getInstance();
        //1.解析args，获取目标服务端口
        AgentClient clientData = this.initClientData(args, isPremain);
        String serverName = clientData.getServiceName();
        if (EnvironmentContext.isInitialized()) {
            // 第二次进入，检查服务名和sid是否一致
            LogUtils.getLogger().warn("Jarboot is already initialized. args: {}, isPremain: {}", args, isPremain);
            return;
        } else {
            String jarbootHome = System.getProperty(CommonConst.JARBOOT_HOME);
            //初始化日志模块
            LogUtils.init(jarbootHome, serverName);

            //2.环境初始化
            EnvironmentContext.init(jarbootHome, clientData, inst);

            //3.initSpy()
            initSpy();

            enhanceClassLoader();
        }

        //4.客户端初始化
        this.initClient();
        //fix: attach本地进程时未初始化而不显示控制台输出的问题，初始化标准输出流
        initStdStream(isPremain);
    }

    public void initClient() {
        if (WsClientFactory.getInstance().checkOnline()) {
            LogUtils.getLogger().warn("当前已经处于在线状态，不需要重新连接");
            return;
        }
        EnvironmentContext.cleanSession();

        WsClientFactory.getInstance().createSingletonClient();
    }

    public boolean isOnline(String host) {
        if (!host.startsWith(CommonConst.HTTP) && !host.startsWith(CommonConst.HTTPS)) {
            host = CommonConst.HTTP + host;
        }
        if (EnvironmentContext.isInitialized()) {
            // 第二次进入，检查是否需要变更Jarboot服务地址
            AgentClient clientData = EnvironmentContext.getAgentClient();
            if (!Objects.equals(host, clientData.getHost())) {
                LogUtils.getLogger().warn("Jarboot host changed: {}, old: {}", host, clientData.getHost());
                if (Boolean.TRUE.equals(clientData.getDiagnose())) {
                    AgentClient client = this.initClientData(host, false);
                    clientData.setSid(client.getSid());
                }
                WsClientFactory.getInstance().changeHost(host);
            }
            String sid = clientData.getSid();
            String pid = PidFileHelper.getServerPidString(sid);
            if (!pid.isEmpty() && !PidFileHelper.PID.equals(pid)) {
                LogUtils.getLogger().warn("pid not match current: {}, pid file: {}", PidFileHelper.PID, pid);
                PidFileHelper.writePidFile(sid);
            }
        } else {
            //以及被执行了shutdown或close命令，此时要重新初始化
            AgentClient client = this.initClientData(host, false);
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

    private void initStdStream(boolean isPremain) {
        StdOutStreamReactor reactor = StdOutStreamReactor.getInstance();
        if (isPremain) {
            //上线成功开启输出流实时显示
            reactor.setStarting();
        }
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
                    LogUtils.getLogger().error("can not find {}", SPY_JAR);
                }
            } catch (Exception e) {
                LogUtils.getLogger().warn(e.getMessage(), e);
            }
        }

        //初始化
        try {
            SpyAPI.init();
        } catch (Exception e) {
            // ignore
        }
    }

    private AgentClient initClientData(String args, boolean isPremain) {
        AgentClient clientData = new AgentClient();
        final String defaultUserDir = "default";
        clientData.setUserDir(defaultUserDir);
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
        if (!host.startsWith(CommonConst.HTTP) && !host.startsWith(CommonConst.HTTPS)) {
            host = CommonConst.HTTP + host;
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
        String url = host + "/api/jarboot/public/agent/agentClient";
        clientData = HttpUtils.postObjByString(url, sb.toString(), AgentClient.class, null);
        if (null == clientData) {
            throw new JarbootException("Request Jarboot server failed! url:" + url);
        }
        if (0 != clientData.getCode()) {
            throw new JarbootException(clientData.getCode(), clientData.getMsg());
        }
        if (StringUtils.isEmpty(clientData.getUserDir())) {
            clientData.setUserDir(defaultUserDir);
        }
        clientData.setHost(host);
        if (null == System.getProperty(CommonConst.REMOTE_PROP, null)) {
            System.setProperty(CommonConst.REMOTE_PROP, host);
        }
        return clientData;
    }

    private boolean initPremainArgs(String args, AgentClient clientData) {
        if (StringUtils.isBlank(args)) {
            return false;
        }
        String s = new String(Base64.getDecoder().decode(args));
        String[] agentArgs = s.split(StringUtils.CR);
        if (agentArgs.length != 4) {
            throw new JarbootException("解析传入传入参数错误！args:" + s);
        }
        clientData.setHost(String.format("%s127.0.0.1:%s", CommonConst.HTTP, agentArgs[0]));
        System.setProperty(CommonConst.REMOTE_PROP, clientData.getHost());
        clientData.setServiceName(agentArgs[1]);
        String sid = agentArgs[2];
        clientData.setSid(sid);
        clientData.setUserDir(agentArgs[3]);
        PidFileHelper.writePidFile(sid);
        return true;
    }

    void enhanceClassLoader() {
        Set<String> loaders = new HashSet<>();
        // 增强 ClassLoader#loadClsss ，解决一些ClassLoader加载不到 SpyAPI的问题
        byte[] classBytes = new byte[0];
        try {
            InputStream inputStream = JarbootBootstrap.class
                    .getClassLoader()
                    .getResourceAsStream(ClassLoader_Instrument.class
                            .getName()
                            .replace('.', '/') + ".class");
            if (null != inputStream) {
                classBytes = IOUtils.getBytes(inputStream);
            }
        } catch (IOException e) {
            LogUtils.getLogger().error(e.getMessage(), e);
            return;
        }

        SimpleClassMatcher matcher = new SimpleClassMatcher(loaders);
        InstrumentConfig instrumentConfig = new InstrumentConfig(AsmUtils.toClassNode(classBytes), matcher);

        InstrumentParseResult instrumentParseResult = new InstrumentParseResult();
        instrumentParseResult.addInstrumentConfig(instrumentConfig);
        InstrumentTransformer classLoaderInstrumentTransformer = new InstrumentTransformer(instrumentParseResult);
        instrumentation.addTransformer(classLoaderInstrumentTransformer, true);

        if (loaders.size() == 1 && loaders.contains(ClassLoader.class.getName())) {
            // 如果只增强 java.lang.ClassLoader，可以减少查找过程
            try {
                instrumentation.retransformClasses(ClassLoader.class);
            } catch (UnmodifiableClassException e) {
                LogUtils.getLogger().error(e.getMessage(), e);
            }
        } else {
            InstrumentationUtils.trigerRetransformClasses(instrumentation, loaders);
        }
    }
}
