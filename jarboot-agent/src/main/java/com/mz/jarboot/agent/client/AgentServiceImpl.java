package com.mz.jarboot.agent.client;

import com.mz.jarboot.agent.JarbootAgent;
import com.mz.jarboot.api.AgentService;
import com.mz.jarboot.common.*;

import java.lang.reflect.Method;

/**
 * Control Service implements<br>
 * 通过Jarboot类加载器，反射操作jarboot-core内部类
 * @author majianzheng
 */
@SuppressWarnings("all")
public class AgentServiceImpl implements AgentService {
    public static final Class<?> CORE_ENV_CLASS;
    private static final String SERVER_NAME;
    private static final Method DISTRIBUTE;

    static {
        Class<?> tmp = null;
        String server = "";
        Method obj = null;
        ClassLoader classLoader = JarbootAgent.getJarbootClassLoader();
        try {
            tmp = classLoader.loadClass("com.mz.jarboot.core.basic.EnvironmentContext");
            server = (String)tmp.getMethod("getServer").invoke(null);
            obj = tmp.getMethod("distributeAction", String.class, String.class, String.class);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
        CORE_ENV_CLASS = tmp;
        SERVER_NAME = server;
        DISTRIBUTE = obj;
    }

    @Override
    public void setStarted() {
        try {
            //启动完成
            CORE_ENV_CLASS.getMethod("setStarted").invoke(null);
        } catch (Exception e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }

    @Override
    public void restartSelf() {
        this.action(CommandConst.ACTION_RESTART, null);
    }

    @Override
    public void notice(String message, String level) {
        action(level, message);
    }

    @Override
    public String getServerName() {
        return SERVER_NAME;
    }

    public static void springContextInit(Object context) {
        try {
            CORE_ENV_CLASS.getMethod("springContextInit", Object.class).invoke(null, context);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }

    private void action(String name, String param) {
        action(name, param, CommandConst.SESSION_COMMON);
    }

    private void action(String name, String param, String sessionId) {
        try {
            DISTRIBUTE.invoke(null, name, param, sessionId);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }
}
