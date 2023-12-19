package io.github.majianzheng.jarboot.agent.client;

import io.github.majianzheng.jarboot.agent.JarbootAgent;
import io.github.majianzheng.jarboot.api.AgentService;

/**
 * Control Service implements<br>
 * 通过Jarboot类加载器，反射操作jarboot-core内部类
 * @author majianzheng
 */
@SuppressWarnings({"squid:S1181", "unused"})
public class AgentServiceImpl implements AgentService {
    public static final Class<?> OPERATOR_CLASS;
    private static final String SERVICE_NAME;
    private static final String SET_STARTED = "setStarted";
    private static final String NOTICE_INFO = "noticeInfo";
    private static final String NOTICE_WARN = "noticeWarn";
    private static final String NOTICE_ERROR = "noticeError";
    private static final String SPRING_INIT = "springContextInit";
    private static final String GET_HOST = "getJarbootHost";

    static {
        Class<?> tmp = null;
        String serviceName = "";
        ClassLoader classLoader = JarbootAgent.getJarbootClassLoader();
        try {
            tmp = classLoader.loadClass("io.github.majianzheng.jarboot.core.basic.AgentServiceOperator");
            serviceName = (String)tmp.getMethod("getServiceName").invoke(null);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
        OPERATOR_CLASS = tmp;
        SERVICE_NAME = serviceName;
    }

    @Override
    public void setStarted() {
        try {
            //启动完成
            OPERATOR_CLASS.getMethod(SET_STARTED).invoke(null);
        } catch (Exception e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }

    @Override
    public void noticeInfo(String message, String sessionId) {
        try {
            OPERATOR_CLASS.getMethod(NOTICE_INFO, String.class, String.class)
                    .invoke(null, message, sessionId);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }

    @Override
    public void noticeWarn(String message, String sessionId) {
        try {
            OPERATOR_CLASS.getMethod(NOTICE_WARN, String.class, String.class)
                    .invoke(null, message, sessionId);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }

    @Override
    public void noticeError(String message, String sessionId) {
        try {
            OPERATOR_CLASS.getMethod(NOTICE_ERROR, String.class, String.class)
                    .invoke(null, message, sessionId);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public String getJarbootHost() {
        try {
            //启动完成
            Object obj = OPERATOR_CLASS.getMethod(GET_HOST).invoke(null);
            if (obj instanceof String) {
                return (String)obj;
            }
        } catch (Exception e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
        return null;
    }

    @Override
    public ClassLoader getJarbootClassLoader() {
        return JarbootAgent.getJarbootClassLoader();
    }

    public static void springContextInit() {
        try {
            OPERATOR_CLASS.getMethod(SPRING_INIT).invoke(null);
        } catch (Throwable e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }
}
