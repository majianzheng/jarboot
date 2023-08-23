package io.github.majianzheng.jarboot.api;

import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;

/**
 * Jarboot Factory
 * @author jianzhengma
 */
public class JarbootFactory {
    private static final String AGENT_CLASS = "io.github.majianzheng.jarboot.agent.client.AgentServiceImpl";
    private static final String SERVICE_MANAGER_CLASS = "io.github.majianzheng.jarboot.client.ServiceManagerClient";
    private static final String SETTING_CLIENT_CLASS = "io.github.majianzheng.jarboot.client.SettingClient";

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
    public static ServiceManager createServiceManager(String host, String user, String password) {
        checkHost(host);
        try {
            Class<?> cls = Class.forName(SERVICE_MANAGER_CLASS);
            return (ServiceManager)cls
                    .getConstructor(String.class, String.class, String.class)
                    .newInstance(host, user, password);
        } catch (Exception e) {
            throw new JarbootRunException(e.getMessage(), e);
        }
    }

    public static SettingService createSettingService(String host, String user, String password) {
        checkHost(host);
        try {
            Class<?> cls = Class.forName(SETTING_CLIENT_CLASS);
            return (SettingService)cls
                    .getConstructor(String.class, String.class, String.class)
                    .newInstance(host, user, password);
        } catch (Exception e) {
            throw new JarbootRunException(e.getMessage(), e);
        }
    }

    public static Object getSpringApplicationContext() {
        return springApplicationContext;
    }

    public static void setSpringApplicationContext(Object context) {
        springApplicationContext = context;
    }

    private static void checkHost(String host) {
        if (null == host || host.isEmpty()) {
            throw new JarbootRunException("host is empty!");
        }
    }

    private JarbootFactory() {
        throw new JarbootRunException("Can not constructor.");
    }
}
