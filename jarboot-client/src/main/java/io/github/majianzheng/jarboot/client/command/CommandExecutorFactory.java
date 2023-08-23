package io.github.majianzheng.jarboot.client.command;

import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.client.ClientProxy;
import io.github.majianzheng.jarboot.client.ServiceManagerClient;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

/**
 * 客户端命令执行器工厂类
 * @author majianzheng
 */
public class CommandExecutorFactory {
    /**
     * Create command executor
     * @param host jarboot host
     * @param username jarboot username
     * @param password jarboot password
     * @return {@link CommandExecutorService}
     */
    public static CommandExecutorService createCommandExecutor(String host,
                                                               String username,
                                                               String password) {
        ClientProxy proxy = ClientProxy.Factory.createClientProxy(host, username, password);
        return createCommandExecutor(proxy, StringUtils.EMPTY);
    }

    /**
     * Create command executor
     * @param service service name
     * @param host jarboot host
     * @param username jarboot username
     * @param password jarboot password
     * @return {@link CommandExecutorService}
     */
    public static CommandExecutorService createCommandExecutor(String service,
                                                               String host,
                                                               String username,
                                                               String password) {
        ClientProxy proxy = ClientProxy.Factory.createClientProxy(host, username, password);
        return createCommandExecutor(service, proxy);
    }

    /**
     * Create command executor
     * @param service service name
     * @param proxy client proxy
     * @return {@link CommandExecutorService}
     */
    public static CommandExecutorService createCommandExecutor(String service, ClientProxy proxy) {
        if (null == proxy) {
            throw new JarbootRunException("Create client proxy failed!");
        }
        ServiceManagerClient serviceManager = new ServiceManagerClient(proxy);
        ServiceInstance instance = serviceManager.getService(service);
        return createCommandExecutor(proxy, instance.getSid());
    }

    /**
     * Create command executor
     * @param proxy client proxy
     * @param sid the service sid, can be null or empty.
     * @return {@link CommandExecutorService}
     */
    public static CommandExecutorService createCommandExecutor(ClientProxy proxy, String sid) {
        if (null == proxy) {
            throw new JarbootRunException("Create client proxy is null!");
        }
        CommandExecutor executor = new CommandExecutor(proxy, sid);
        executor.client = CommandExecutor.connect(proxy.getHost(), proxy.getToken(), executor);
        return executor;
    }

    private CommandExecutorFactory() {}
}
