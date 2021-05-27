package com.mz.jarboot.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;

public class JarbootAgent {// NOSONAR
    private static final String JARBOOT_CORE_JAR = "jarboot-core.jar";
    private static final String JARBOOT_CLASS = "com.mz.jarboot.core.server.JarbootBootstrap";
    private static final String GET_INSTANCE = "getInstance";
    private static volatile boolean INITED = false; // NOSONAR

    private static PrintStream ps = System.err; // NOSONAR

    static {
        try {
            File logDir = new File(System.getProperty("user.home") + File.separator +
                    "jarboot" + File.separator + "logs"  + File.separator);
            if (!logDir.exists()) {
                logDir.mkdir(); // NOSONAR
            }
            File log = new File(logDir, "jarboot-agent.log");
            if (!log.exists()) {
                log.createNewFile(); // NOSONAR
            }
            ps = new PrintStream(new FileOutputStream(log, false));
        } catch (Throwable e) { // NOSONAR
            e.printStackTrace(ps);
        }
    }

    private static volatile ClassLoader jarbootClassLoader = null; // NOSONAR

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    private static ClassLoader getClassLoader(File jarFile) throws MalformedURLException {
        if (null == jarbootClassLoader) {
            jarbootClassLoader = new JarbootClassLoader(new URL[]{jarFile.toURI().toURL()});
        }
        return jarbootClassLoader;
    }

    private static void clientCheckAndInit() {
        try {
            Class<?> bootClass = jarbootClassLoader.loadClass(JARBOOT_CLASS);
            //获取实例
            Object inst = bootClass.getMethod(GET_INSTANCE).invoke(null);
            boolean isOnline = (boolean)bootClass.getMethod("isOnline").invoke(inst);
            if (isOnline) {
                ps.println("Agent客户端已经处于在线状态");
            } else {
                ps.println("Agent客户端不在线，开始重新连接中...");
                bootClass.getMethod("initClient").invoke(inst);
                ps.println("Agent客户端重新连接完成！");
            }
        } catch (Exception e) {
            e.printStackTrace(ps);
        }
    }

    private static synchronized void main(String args, final Instrumentation inst) {
        if (INITED) {
            ps.println("jarboot Agent is already started!");
            //检查是否在线
            clientCheckAndInit();
            return;
        }
        ps.println("jarboot Agent start...");

        CodeSource codeSource = JarbootAgent.class.getProtectionDomain().getCodeSource();
        File coreJarFile;
        try {
            File agentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
            coreJarFile = new File(agentJarFile.getParentFile(), JARBOOT_CORE_JAR);
            if (!coreJarFile.exists()) {
                ps.println("Can not find jarboot-core jar file." + agentJarFile);
            }
        } catch (Throwable e) {// NOSONAR
            ps.println("Can not find jar file from" + codeSource.getLocation());
            e.printStackTrace(ps);
            return;
        }
        if (!coreJarFile.exists()) {
            return;
        }
        try {
            //构造自定义的类加载器
            ClassLoader classLoader = getClassLoader(coreJarFile);

            bind(classLoader, inst, args);
            //初始化成功
            INITED = true;
            ps.println("jarboot Agent ready.");
        } catch (Throwable e) {// NOSONAR
            e.printStackTrace(ps);
        }
    }
    private static void bind(ClassLoader classLoader, Instrumentation inst, String args) throws Exception {// NOSONAR
        Class<?> bootClass = classLoader.loadClass(JARBOOT_CLASS);
        bootClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class).invoke(null, inst, args);
    }
}
