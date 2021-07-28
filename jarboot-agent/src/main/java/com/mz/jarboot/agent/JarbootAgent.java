package com.mz.jarboot.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.jarboot.SpyAPI;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public class JarbootAgent {
    private static final String JARBOOT_CORE_JAR = "jarboot-core.jar";
    private static final String JARBOOT_CLASS = "com.mz.jarboot.core.server.JarbootBootstrap";
    private static final String GET_INSTANCE = "getInstance";

    private static PrintStream ps = System.err;
    private static final File CURRENT_DIR;

    static {
        CURRENT_DIR = getCurrentDir();
        try {
            File logDir = new File(CURRENT_DIR, "logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            File log = new File(logDir, "jarboot-agent.log");
            if (!log.exists()) {
                log.createNewFile();
            }
            ps = new PrintStream(new FileOutputStream(log, false));
        } catch (Throwable e) {
            e.printStackTrace(ps);
        }
    }

    private static volatile ClassLoader jarbootClassLoader = null; // NOSONAR

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
        //上线成功开启输出流实时显示
        try {
            Class<?> bootClass = jarbootClassLoader.loadClass(JARBOOT_CLASS);
            Object obj = bootClass.getMethod(GET_INSTANCE).invoke(null);
            boolean isOnline = (Boolean) bootClass.getMethod("isOnline").invoke(obj);
            if (isOnline) {
                bootClass.getMethod("setStarting").invoke(obj);
            }
        } catch (Exception e) {
            e.printStackTrace(ps);
        }
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
            boolean isOnline = (Boolean) bootClass.getMethod("isOnline").invoke(inst);
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
        try {
            Class.forName("java.jarboot.SpyAPI");
            if (SpyAPI.isInited()) {
                ps.println("Jarboot Agent is already started, skip attach and check client.");
                //检查是否在线
                clientCheckAndInit();
                ps.flush();
                return;
            }
        } catch (Exception e) {
            // ignore
        }

        ps.println("jarboot Agent start...");

        CodeSource codeSource = JarbootAgent.class.getProtectionDomain().getCodeSource();
        File coreJarFile;
        try {
            coreJarFile = new File(CURRENT_DIR, JARBOOT_CORE_JAR);
            if (!coreJarFile.exists()) {
                ps.println("Can not find jarboot-core jar file." + coreJarFile.getPath());
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
            ps.println("jarboot Agent ready.");
        } catch (Throwable e) {
            e.printStackTrace(ps);
        }
    }
    private static void bind(ClassLoader classLoader, Instrumentation inst, String args) throws Exception {
        Class<?> bootClass = classLoader.loadClass(JARBOOT_CLASS);
        bootClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class).invoke(null, inst, args);
    }

    private static File getCurrentDir() {
        CodeSource codeSource = JarbootAgent.class.getProtectionDomain().getCodeSource();
        try {
            File agentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
            return agentJarFile.getParentFile();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
