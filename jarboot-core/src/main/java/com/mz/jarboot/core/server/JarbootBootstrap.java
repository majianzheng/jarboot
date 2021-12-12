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
        ClientData clientData = this.initClientData(args, isPremain);
        String sid = clientData.getSid();
        String serverName = clientData.getServer();
        if (EnvironmentContext.isInitialized()) {
            // 第二次进入，检查服务名和sid是否一致
            logger.warn("Jarboot is already initialized. args: {}, isPremain: {}", args, isPremain);
            return;
        } else {
            String jarbootHome = System.getProperty(CommonConst.JARBOOT_HOME);
            //初始化日志模块
            boolean persist = CommonConst.INVALID_PID != PidFileHelper.getServerPid(sid);
            LogUtils.init(jarbootHome, serverName, sid, persist);
            logger = LogUtils.getLogger();

            //2.环境初始化
            EnvironmentContext.init(jarbootHome, clientData, inst);

            //3.initSpy()
            initSpy();

            enhanceClassLoader();
        }

        //4.客户端初始化
        this.initClient();
        if (clientData.isHostRemote()) {
            WsClientFactory.getInstance().remoteJvmSchedule();
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
        if (WsClientFactory.getInstance().isOnline()) {
            logger.warn("当前已经处于在线状态，不需要重新连接");
            return;
        }
        EnvironmentContext.cleanSession();

        WsClientFactory.getInstance().createSingletonClient();
    }

    public boolean isOnline(String host) {
        if (EnvironmentContext.isInitialized()) {
            ClientData clientData = EnvironmentContext.getClientData();
            // 第二次进入，检查是否需要变更Jarboot服务地址
            //若存在pid文件，则说明是由jarboot管理的进程，忽略变化
            if (!Objects.equals(host, clientData.getHost())) {
                if (checkIsLacalAddr(host)) {
                    clientData.setSid(PidFileHelper.PID);
                    clientData.setHostRemote(false);
                }
                WsClientFactory.getInstance().changeHost(host);
            }
            String sid = clientData.getSid();
            int pid = PidFileHelper.getServerPid(sid);
            if (CommonConst.INVALID_PID != pid && !PidFileHelper.PID.equals(String.valueOf(pid))) {
                logger.warn("pid not match current: {}, pid file: {}", PidFileHelper.PID, pid);
                PidFileHelper.writePidFile(sid);
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
        if (isPremain) {
            //由jarboot本地启动时，解析传入参数
            initPremainArgs(args, clientData);
        }

        //设定Host
        if (StringUtils.isBlank(clientData.getHost())) {
            String remote = System.getProperty(CommonConst.REMOTE_PROP, null);
            if (StringUtils.isEmpty(remote) && !StringUtils.isEmpty(args)) {
                remote = args;
                System.setProperty(CommonConst.REMOTE_PROP, remote);
            }
            clientData.setHost(remote);
        }

        //设定服务名
        if (StringUtils.isBlank(clientData.getServer())) {
            //获取服务名
            String serverName = System.getProperty(CommonConst.SERVER_NAME_PROP, null);
            if (null == serverName) {
                serverName = System.getProperty("sun.java.command", "Name-" + PidFileHelper.PID);
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
        }

        //设定sid
        if (StringUtils.isBlank(clientData.getSid())) {
            //获取远程的Jarboot服务地址
            if (checkIsLacalAddr(clientData.getHost())) {
                //本地进程，使用pid作为sid
                clientData.setSid(PidFileHelper.PID);
                clientData.setHostRemote(false);
            } else {
                //先产生一个未知的hash然后再和nanoTime组合，减少sid重合的几率
                int h = Objects.hash(PidFileHelper.INSTANCE_NAME, System.nanoTime());
                //远程进程，使用pid + ip + name + hash地址作为sid
                String ip = getLocalAddr();
                StringBuilder sb = new StringBuilder();
                sb
                        .append(CommonConst.REMOTE_SID_PREFIX)
                        .append(CommonConst.COMMA_SPLIT)
                        .append(PidFileHelper.PID)
                        .append(CommonConst.COMMA_SPLIT)
                        .append(ip)
                        .append(CommonConst.COMMA_SPLIT)
                        .append(clientData.getServer())
                        .append(CommonConst.COMMA_SPLIT)
                        .append(String.format("%x-%x", h, System.nanoTime()));
                clientData.setSid(sb.toString());
                clientData.setHostRemote(true);
            }
        }

        return clientData;
    }

    private void initPremainArgs(String args, ClientData clientData) {
        if (StringUtils.isBlank(args)) {
            return;
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
    }

    /**
     * 获取本机网卡的地址
     * @return 网卡地址
     */
    private String getLocalAddr() {
        //先尝试获取IPv4的地址
        List<String> addrList = NetworkUtils.getLocalAddr4();
        if (null != addrList && !addrList.isEmpty()) {
            for (String addr : addrList) {
                //.1结尾的可能是虚拟机的虚拟网卡地址
                if (!addr.endsWith(".1") && !addr.contains(".0.")) {
                    return addr;
                }
            }
            for (String addr : addrList) {
                //.0.*的可能是交换地址，如docker
                if (!addr.contains(".0.")) {
                    return addr;
                }
            }
            return addrList.get(0);
        }
        //IPv4地址为空时，使用IPv6的地址
        addrList = NetworkUtils.getLocalAddr();
        if (null == addrList || addrList.isEmpty()) {
            throw new JarbootException("获取本机IP地址异常！");
        }
        return addrList.get(0);
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
