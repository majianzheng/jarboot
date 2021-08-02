package com.mz.jarboot.api;

import com.mz.jarboot.api.exception.JarbootRunException;

/**
 * Jarboot Factory
 * @author jianzhengma
 */
public class JarbootFactory {
    private static final String AGENT_CLASS = "com.mz.jarboot.agent.client.AgentServiceImpl";

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

    private JarbootFactory() {
        throw new JarbootRunException("Can not constructor.");
    }
}
