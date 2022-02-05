package com.mz.jarboot.client.command;

import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServiceInstance;

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
     * @param serviceId The service id, use {@link com.mz.jarboot.client.ServiceManagerClient#getService(String)} to get.
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
     * Can use {@link com.mz.jarboot.client.ServiceManagerClient#getJvmProcesses()} get the instance info or <br>
     * use {@link com.mz.jarboot.client.ServiceManagerClient#getService(String)} to get the service sid.
     * @param serviceId sid
     */
    void switchInstance(String serviceId);

    /**
     * List the services <br>
     * {@link com.mz.jarboot.client.ServiceManagerClient#getServiceList()}
     * @param filter filter
     * @return services
     */
    List<ServiceInstance> listServices(String filter);

    /**
     * List the jvm instances <br>
     * {@link com.mz.jarboot.client.ServiceManagerClient#getJvmProcesses()}
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
     * But there is useless when the executor is already shutdown.
     */
    void tryReconnect();

    /**
     * shutdown executor
     */
    void shutdown();
}
