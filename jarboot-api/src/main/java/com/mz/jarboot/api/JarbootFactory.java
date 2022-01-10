package com.mz.jarboot.api;

import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.api.service.ServerMgrService;

/**
 * Jarboot Factory
 * @author jianzhengma
 */
public class JarbootFactory {
    private static final String AGENT_CLASS = "com.mz.jarboot.agent.client.AgentServiceImpl";
    private static final String SERVER_MANAGER_CLASS = "com.mz.jarboot.client.ServerManager";

    private static Object springApplicationContext = null;
    /**
     * 创建AgentService实例<br>
     * 前置条件：使用Jarboot启动的进程，否则抛出异常{@link JarbootRunException}，调用端代码要做好异常防护
     * @return {@link AgentService}
     */
    public static AgentService createAgentService() {
        try {
            Class<?> cls = Class.forName(AGENT_CLASS);
            return (AgentService)cls.getConstructor().newInstance();
        } catch (Exception e) {
            throw new JarbootRunException("Current application maybe not started by jarboot", e);
        }
    }

    /**
     * 创建服务管理客户端
     * 需要引入jarboot-client包
     * @param host Jarboot服务地址
     * @param user 用户名
     * @param password 用户密码
     * @return 服务管理客户端
     */
    public static ServerMgrService createServerManager(String host, String user, String password) {
        try {
            Class<?> cls = Class.forName(SERVER_MANAGER_CLASS);
            return (ServerMgrService)cls
                    .getConstructor(String.class, String.class, String.class)
                    .newInstance(host, user, password);
        } catch (Exception e) {
            throw new JarbootRunException("Current application maybe not started by jarboot", e);
        }
    }

    public static Object getSpringApplicationContext() {
        return springApplicationContext;
    }

    public static void setSpringApplicationContext(Object context) {
        springApplicationContext = context;
    }

    private JarbootFactory() {
        throw new JarbootRunException("Can not constructor.");
    }
}
