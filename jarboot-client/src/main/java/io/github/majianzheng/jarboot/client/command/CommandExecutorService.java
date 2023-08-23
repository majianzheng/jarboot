package io.github.majianzheng.jarboot.client.command;

import io.github.majianzheng.jarboot.api.pojo.JvmProcess;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.client.ServiceManagerClient;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Execute diagnose command. <br>
 * @author majianzheng
 */
public interface CommandExecutorService {
    /**
     * Execute command
     * @param cmd command line
     * @param callback event callback
     * @return result future
     */
    Future<CommandResult> execute(String cmd, NotifyCallback callback);

    /**
     * Execute command
     * @param serviceId The service id, use {@link ServiceManagerClient#getService(String)} to get.
     * @param cmd command line
     * @param callback event callback
     * @return result future
     */
    Future<CommandResult> execute(String serviceId, String cmd, NotifyCallback callback);

    /**
     * Force cancel current executing command.
     * @param serviceId service id
     */
    void forceCancel(String serviceId);

    /**
     * Switch current instance by service
     * @param service service name
     */
    void switchService(String service);

    /**
     * Switch current instance
     * Can use {@link ServiceManagerClient#getJvmProcesses()} get the instance info or <br>
     * use {@link ServiceManagerClient#getService(String)} to get the service sid.
     * @param serviceId sid
     */
    void switchInstance(String serviceId);

    /**
     * List the services <br>
     * {@link ServiceManagerClient#getServiceList()}
     * @param filter filter
     * @return services
     */
    List<ServiceInstance> listServices(String filter);

    /**
     * List the jvm instances <br>
     * {@link ServiceManagerClient#getJvmProcesses()}
     * @param filter filter
     * @return jvm instances
     */
    List<JvmProcess> listJvmInstances(String filter);

    /**
     * Get current instance sid
     * @return sid
     */
    String getCurrentSid();

    /**
     * Check current client is online now.
     * @return online
     */
    boolean checkOnline();

    /**
     * Try reconnect to jarboot server when not online. <br>
     * If check {@link #checkOnline()} returns false, then can use this method to reconnect.
     */
    void tryReconnect();

    /**
     * shutdown executor
     */
    void shutdown();
}
