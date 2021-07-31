package com.mz.jarboot.agent.client;

import com.mz.jarboot.agent.JarbootAgent;
import com.mz.jarboot.api.AgentService;

/**
 * Control Service implements
 * @author majianzheng
 */
public class AgentServiceImpl implements AgentService {
    private static final String BASE_API = "/api/public/agent/";
    private static final Class<?> ENV_CLASS;
    private static final String SET_STARTED_API;
    private static final String SERVER_NAME;

    static {
        Class<?> tmp = null;
        String api = "";
        String server = "";
        try {
            tmp = JarbootAgent.getJarbootClassLoader()
                    .loadClass("com.mz.jarboot.core.basic.EnvironmentContext");
            server = (String)tmp.getMethod("getServer").invoke(null);
            api = BASE_API + "setStarted?server=" + server;
        } catch (Exception e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
        ENV_CLASS = tmp;
        SET_STARTED_API = api;
        SERVER_NAME = server;
    }
    @Override
    public void setStarted() {
        if (isStarted()) {
            return;
        }
        try {
            Class<?> httpCls = JarbootAgent.getJarbootClassLoader()
                    .loadClass("com.mz.jarboot.core.utils.HttpUtils");
            httpCls.getMethod("getSimple", String.class).invoke(null, SET_STARTED_API);
            //启动完成
            ENV_CLASS.getMethod("setStarted").invoke(null);
        } catch (Exception e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
    }

    @Override
    public String getServerName() {
        return SERVER_NAME;
    }

    private boolean isStarted() {
        boolean isStarted = false;
        try {
            isStarted = (boolean)ENV_CLASS.getMethod("isStarted").invoke(null);
        } catch (Exception e) {
            e.printStackTrace(JarbootAgent.getPs());
        }
        return isStarted;
    }
}
