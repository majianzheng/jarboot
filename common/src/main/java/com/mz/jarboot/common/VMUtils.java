package com.mz.jarboot.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VMUtils {
    private static Method attach;
    private static Method loadAgent;
    private static Method detach;
    private static Method listVM;
    private static Method getVMId;
    private static Method getVMName;
    private static volatile VMUtils instance = null;
    public static VMUtils getInstance() {
        if (null == instance) {
            synchronized (VMUtils.class) {
                if (null == instance) {
                    instance = new VMUtils();
                }
            }
        }
        return instance;
    }
    public Object attachVM(int pid) throws Exception {
        return attach.invoke(null, String.valueOf(pid));
    }

    public void loadAgentToVM(Object vm, String path, String args) throws Exception {
        loadAgent.invoke(vm, path, args);
    }

    public Map<Integer, String> listVM() {
        Map<Integer, String> vmMap = new HashMap<>();
        List<?> list;
        try {
            list = (List<?>) listVM.invoke(null);
        } catch (Exception e) {
            //ignore
            return vmMap;
        }

        list.forEach(vmd -> {
            try {
                String id = (String) getVMId.invoke(vmd);
                String name = (String) getVMName.invoke(vmd);

                vmMap.put(Integer.parseInt(id), name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return vmMap;
    }

    public void detachVM(Object vm) {
        try {
            detach.invoke(vm);
        } catch (Exception e) {
            //ignore
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

        try {
            if (null == vmClass || null == vmdClass) {
                //当前可能是用jre在运行，尝试加载tools.jar
                URL url = getVMToolsUrl();
                ClassLoader classLoader = new URLClassLoader(new URL[]{url});
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


    private VMUtils() {
        try {
            init();
        } catch (Exception e) {
            //未成功加载到tools.jar，运行的可能是在jre上
            System.exit(-1);
        }
    }
}
