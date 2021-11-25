package com.mz.jarboot.core.server;

import ch.qos.logback.classic.Logger;
import com.alibaba.bytekit.asm.instrument.InstrumentConfig;
import com.alibaba.bytekit.asm.instrument.InstrumentParseResult;
import com.alibaba.bytekit.asm.instrument.InstrumentTransformer;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.IOUtils;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.NetworkUtils;
import com.mz.jarboot.common.PidFileHelper;
import com.mz.jarboot.core.basic.ClientData;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.basic.WsClientFactory;
import com.mz.jarboot.core.stream.StdOutStreamReactor;
import com.mz.jarboot.core.utils.InstrumentationUtils;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.jarboot.SpyAPI;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.CodeSource;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
        ClientData clientData = this.initClientData(args, isPremain);
        String sid = clientData.getSid();
        String serverName = clientData.getServer();
        if (EnvironmentContext.isInitialized()) {
            // 第二次进入，检查服务名和sid是否一致
            if (!sid.equals(EnvironmentContext.getSid())) {
                logger.error("Attach failed, server {}@{} not match current {}@{}!",
                        serverName, sid, EnvironmentContext.getServer(), EnvironmentContext.getSid());
                //删除pid文件
                PidFileHelper.deletePidFile(sid);
                return;
            }
        } else {
            String jarbootHome = System.getProperty(CommonConst.JARBOOT_HOME);
            LogUtils.init(jarbootHome, serverName, sid, isPremain); //初始化日志模块
            logger = LogUtils.getLogger();

            //2.环境初始化
            EnvironmentContext.init(jarbootHome, clientData, inst);

            //3.initSpy()
            initSpy();

            enhanceClassLoader();
        }

        //4.客户端初始化
        this.initClient();
        if (clientData.isHostRemoted()) {
            WsClientFactory.getInstance().remoteJvm();
        }
        if (isPremain) {
            //上线成功开启输出流实时显示
            StdOutStreamReactor.getInstance().setStarting();
        }
    }

    public void initClient() {
        if (WsClientFactory.getInstance().isOnline()) {
            logger.warn("当前已经处于在线状态，不需要重新连接");
            return;
        }
        EnvironmentContext.cleanSession();

        WsClientFactory.getInstance().createSingletonClient();
    }

    public boolean isOnline(String args) {
        ClientData clientData = initClientData(args, false);
        String sid = clientData.getSid();
        if (EnvironmentContext.isInitialized()) {
            // 第二次进入，检查服务名和sid是否一致
            if (!sid.equals(EnvironmentContext.getSid())) {
                logger.error("Attach failed, server {}@{} not match current {}@{}!",
                        clientData.getServer(),
                        sid,
                        EnvironmentContext.getServer(),
                        EnvironmentContext.getSid());
                //删除pid文件
                PidFileHelper.deletePidFile(sid);
                return true;
            }
        }
        return WsClientFactory.getInstance().isOnline();
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

    private ClientData initClientData(String args, boolean isPremain) {
        ClientData clientData = new ClientData();
        if (StringUtils.isBlank(args)) {
            String remote = System.getProperty(CommonConst.REMOTE_PROP, null);
            if (checkIsLacalAddr(remote)) {
                //本地进程，使用pid作为sid
                clientData.setSid(PidFileHelper.getCurrentPid());
                clientData.setHostRemoted(false);
            } else {
                //远程进程，使用uuid作为sid
                clientData.setSid(CommonConst.REMOTE_SID_PREFIX + UUID.randomUUID().toString());
                clientData.setHostRemoted(true);
            }
            clientData.setHost(remote);
            String serverName = System.getProperty(CommonConst.SERVER_NAME_PROP, null);
            if (null == serverName) {
                serverName = System.getProperty("sun.java.command", "NoName");
                int p = serverName.indexOf(' ');
                if (p > 0) {
                    serverName = serverName.substring(0, p);
                }
                final char sept = (serverName.endsWith(CommonConst.JAR_EXT)) ? File.separatorChar : '.';
                int index = serverName.lastIndexOf(sept);
                if (index > 0) {
                    serverName = serverName.substring(index + 1);
                }
            }

            clientData.setServer(serverName);
            return clientData;
        }
        //由jarboot本地启动
        String s = new String(Base64.getDecoder().decode(args));
        String[] agentArgs = s.split(String.valueOf(CommandConst.PROTOCOL_SPLIT));
        if (agentArgs.length != 3) {
            throw new JarbootException("解析传入传入参数错误！args:" + s);
        }
        clientData.setHost(String.format("127.0.0.1:%s", agentArgs[0]));
        clientData.setServer(agentArgs[1]);
        String sid = agentArgs[2];
        clientData.setSid(sid);
        if (isPremain) {
            PidFileHelper.writePidFile(sid);
        }
        return clientData;
    }

    private boolean checkIsLacalAddr(String remote) {
        if (StringUtils.isBlank(remote)) {
            throw new JarbootException("未指定要连接的jarboot服务，jarboot.remote为空！");
        }
        int index = remote.lastIndexOf(':');
        if (-1 == index) {
            throw new JarbootException("传入的jarboot.remote格式错误，remote:" + remote);
        }
        String addr = remote.substring(0, index);
        return NetworkUtils.hostLocal(addr);
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
