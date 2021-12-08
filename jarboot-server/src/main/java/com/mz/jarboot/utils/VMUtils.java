package com.mz.jarboot.utils;

import com.mz.jarboot.common.JarbootException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VM操作，为了兼容多种jdk版本的编译，使用了反射的方式
 * @author majianzheng
 */
@SuppressWarnings("all")
public class VMUtils {
    private static final Logger logger = LoggerFactory.getLogger(VMUtils.class);
    /** attach方法 */
    private Method attach;
    /** loadAgent方法 */
    private Method loadAgent;
    /** detach方法 */
    private Method detach;
    /** 枚举本地JVM实例方法 */
    private Method listVM;
    /** 获取JVM的PID方法 */
    private Method getVMId;
    /** 获取JVM的名字方法 */
    private Method getVMName;
    /** 是否初始化成功 */
    private boolean initialized = false;

    /**
     * 获取单例
     * @return 单例
     */
    public static VMUtils getInstance() {
        return VMUtilsHolder.INSTANCE;
    }

    /**
     * 是否初始化成功
     * @return 是否初始化成功
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Attach指定的VM
     * @param pid 进程PID
     * @return VM实例
     */
    public Object attachVM(int pid) {
        try {
            return attach.invoke(null, String.valueOf(pid));
        } catch (Exception e) {
            throw new JarbootException("Attach failed! " + e.getMessage(), e);
        }
    }

    /**
     * loadAgent
     * @param vm 指定的VM实例
     * @param path agent的jar文件路径
     * @param args 参数
     */
    public void loadAgentToVM(Object vm, String path, String args) {
        try {
            loadAgent.invoke(vm, path, args);
        } catch (Exception e) {
            throw new JarbootException("loadAgent failed! " + e.getMessage(), e);
        }
    }

    /**
     * 枚举本地的VM实例
     * @return VM实例列表
     */
    public Map<Integer, String> listVM() {
        HashMap<Integer, String> vmMap = new HashMap<>();
        List<?> list;
        try {
            list = (List<?>) listVM.invoke(null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return vmMap;
        }

        list.forEach(vmd -> {
            try {
                String id = (String) getVMId.invoke(vmd);
                String name = (String) getVMName.invoke(vmd);

                vmMap.put(Integer.parseInt(id), name);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
        return vmMap;
    }

    /**
     * Detach VM
     * @param vm vm实例
     */
    public void detachVM(Object vm) {
        try {
            detach.invoke(vm);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void init() {
        final String vmClassName = "com.sun.tools.attach.VirtualMachine";
        final String vmdClassName = "com.sun.tools.attach.VirtualMachineDescriptor";
        Class<?> vmClass = null;
        Class<?> vmdClass = null;
        try {
            vmClass = Class.forName(vmClassName);
            vmdClass = Class.forName(vmdClassName);
        } catch (ClassNotFoundException e) {
            //ignore
        }
        try {// NOSONAR
            if (null == vmClass || null == vmdClass) {
                //当前可能是用jre在运行，尝试加载tools.jar
                URL url = getVMToolsUrl();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{url});// NOSONAR
                vmClass = classLoader.loadClass(vmClassName);
                vmdClass = classLoader.loadClass(vmdClassName);
            }
            attach = vmClass.getMethod("attach", String.class);
            loadAgent = vmClass.getMethod("loadAgent", String.class, String.class);
            detach = vmClass.getMethod("detach");
            listVM = vmClass.getMethod("list");
            getVMId = vmdClass.getMethod("id");
            getVMName = vmdClass.getMethod("displayName");
        } catch (Exception e) {
            //加载jdk的tools.jar失败
            throw new IllegalStateException("Load tools.jar failed, make sure you are using a jdk not a jre.", e);
        }
    }

    private URL getVMToolsUrl() throws FileNotFoundException, MalformedURLException {
        //尝试获取JAVA_HOME环境变量和当前运行环境，以获取jdk的tools.jar的位置，通过反射加载其中的VM类
        String jdkHome = System.getenv("JAVA_HOME");
        String toolsJarFilePath = null;
        File tools;
        if (null == jdkHome || jdkHome.isEmpty()) {
            toolsJarFilePath = jdkHome + File.separator + "lib" + File.separator + "tools.jar";
            tools = new File(toolsJarFilePath);
            if (!tools.exists()) {
                toolsJarFilePath = null;
            }
        }
        if (null == toolsJarFilePath) {
            String path = System.getProperty("java.home");
            int p = path.indexOf(File.separator + "jre");
            if (-1 != p) {
                //当前在jre路径，则切换到上级的jdk路径
                path = path.substring(0, p);
            }
            toolsJarFilePath = (path + File.separator + "lib" + File.separator + "tools.jar");
        }
        tools = new File(toolsJarFilePath);
        if (!tools.exists()) {
            throw new FileNotFoundException("Can not find tools.jar, make sure you are using a jdk not a jre.");
        }
        return tools.toURI().toURL();
    }

    private static class VMUtilsHolder {
        static final VMUtils INSTANCE = new VMUtils();
    }

    private VMUtils() {
        try {
            init();
            initialized = true;
        } catch (Exception e) {
            //未成功加载到tools.jar，运行的可能是在jre上
            logger.error(e.getMessage(), e);
        }
    }
}
