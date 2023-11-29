package io.github.majianzheng.jarboot.agent;

import io.github.majianzheng.jarboot.api.constant.CommonConst;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.jarboot.SpyAPI;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.jar.JarFile;

/**
 * @author majianzheng
 */
@SuppressWarnings({"squid:S1118", "squid:S106", "squid:S1181", "squid:3077", "squid:S2093", "squid:S899"})
public class JarbootAgent {
    private static final String JARBOOT_CORE_JAR = "jarboot-core.jar";
    private static final String JARBOOT_CLASS = "io.github.majianzheng.jarboot.core.server.JarbootBootstrap";
    private static final String GET_INSTANCE = "getInstance";

    private static PrintStream ps = null;
    private static final String CURRENT_DIR = getCurrentDir();
    private static ClassLoader jarbootClassLoader = null;

    public static void premain(String args, Instrumentation inst) {
        callMain(args, inst, true);
    }

    public static void agentmain(String args, Instrumentation inst) {
        callMain(args, inst, false);
    }

    private static void callMain(String args, Instrumentation inst, boolean isPremain) {
        try {
            //初始化临时日志
            File logDir = new File(CURRENT_DIR, "logs");
            if (!logDir.exists() && !logDir.mkdir()) {
                System.out.println("mkdir logs failed!");
            }
            File log = new File(logDir, "jarboot-agent.log");
            if (!log.exists() && !log.createNewFile()) {
                System.out.println("create jarboot agent log failed.");
            }
            ps = new PrintStream(new FileOutputStream(log, false));
            main(args, inst, isPremain);
        } catch (Throwable e) {
            e.printStackTrace(null == ps ? System.out : ps);
        } finally {
            if (null != ps) {
                try {
                    ps.close();
                } catch (Throwable e) {
                    e.printStackTrace(ps);
                } finally {
                    ps = null;
                }
            }
        }
    }

    /**
     * 获取{@link JarbootClassLoader}，供扩展的反射使用，通过此方法获取的加载器可获取jarboot-core的内部类
     * @return ClassLoader
     */
    public static ClassLoader getJarbootClassLoader() {
        return jarbootClassLoader;
    }

    public static PrintStream getPs() {
        return ps;
    }

    private static ClassLoader getClassLoader(URL[] urls) {
        if (null == jarbootClassLoader) {
            jarbootClassLoader = new JarbootClassLoader(urls);
        }
        return jarbootClassLoader;
    }

    private static void clientCheckAndInit(String args) {
        try {
            Class<?> bootClass = jarbootClassLoader.loadClass(JARBOOT_CLASS);
            //获取实例
            Object inst = bootClass.getMethod(GET_INSTANCE).invoke(null);
            boolean isOnline = (Boolean) bootClass.getMethod("isOnline", String.class).invoke(inst, args);
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

    private static synchronized void main(String args, final Instrumentation inst, boolean isPremain) {
        try {
            Class.forName("java.jarboot.SpyAPI");
            if (SpyAPI.isInited()) {
                //非正常流程，第二次或第n次attach
                ps.println("Jarboot Agent is already started, skip attach and check client.");
                //检查是否在线
                clientCheckAndInit(args);
                ps.flush();
                return;
            }
        } catch (Exception e) {
            //ignore 正常流程，当前第一次attach
        }

        ps.println("jarboot Agent start...");

        CodeSource codeSource = JarbootAgent.class.getProtectionDomain().getCodeSource();
        File coreJarFile;
        try {
            coreJarFile = new File(CURRENT_DIR + File.separator + CommonConst.COMPONENTS_NAME, JARBOOT_CORE_JAR);
            if (!coreJarFile.exists()) {
                ps.println("Can not find jarboot-core jar file." + coreJarFile.getPath());
            }
        } catch (Throwable e) {
            ps.println("Can not find jar file from" + codeSource.getLocation());
            e.printStackTrace(ps);
            return;
        }
        if (!coreJarFile.exists()) {
            return;
        }
        try {
            //初始化plugins
            initAgentPlugins(inst);
            //加载core
            URL[] urls = getClassLoaderUrls(coreJarFile);
            //构造自定义的类加载器
            ClassLoader classLoader = getClassLoader(urls);
            bind(classLoader, inst, args, isPremain);
            //初始化成功
            ps.println("jarboot Agent ready.");
        } catch (Throwable e) {
            e.printStackTrace(ps);
        }
    }
    
    private static URL[] getClassLoaderUrls(File coreFile) {
        URL[] urls = new URL[1];
        try {
            urls[0] = coreFile.toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace(ps);
        }
        return urls;
    }
    
    private static void initAgentPlugins(Instrumentation inst) {
        File agentPluginsDir = new File(CURRENT_DIR + File.separator + "plugins", "agent");
        File[] jarFiles = agentPluginsDir.listFiles(file -> file.getName().endsWith(".jar"));
        if (null != jarFiles && jarFiles.length > 0) {
            for (File jarFile : jarFiles) {
                try {
                    JarFile jar = new JarFile(jarFile);
                    inst.appendToSystemClassLoaderSearch(jar);
                } catch (IOException e) {
                    e.printStackTrace(ps);
                }
            }
        }
    }
    
    private static void bind(ClassLoader classLoader, Instrumentation inst, String args, boolean isPremain)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> bootClass = classLoader.loadClass(JARBOOT_CLASS);
        bootClass
                .getMethod(GET_INSTANCE, Instrumentation.class, String.class, boolean.class)
                .invoke(null, inst, args, isPremain);
    }

    private static String getCurrentDir() {
        //分别尝试从系统属性和环境变量中获取
        String homePath = System.getProperty(CommonConst.JARBOOT_HOME, System.getenv(CommonConst.JARBOOT_HOME));
        if (null != homePath && !homePath.isEmpty()) {
            if (null == System.getProperty(CommonConst.JARBOOT_HOME, null)) {
                //将环境变量中的设置
                System.setProperty(CommonConst.JARBOOT_HOME, homePath);
            }
            return homePath;
        }
        CodeSource codeSource = JarbootAgent.class.getProtectionDomain().getCodeSource();
        try {
            File agentJarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
            homePath = agentJarFile.getParentFile().getParentFile().getAbsolutePath();
            System.setProperty(CommonConst.JARBOOT_HOME, homePath);
        } catch (Exception e) {
            e.printStackTrace(ps);
        }
        return homePath;
    }
}
