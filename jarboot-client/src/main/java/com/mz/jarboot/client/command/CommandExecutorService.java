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
     * execute command
     * @param cmd command line
     * @param callback event callback
     * @return result future
     */
    Future<CommandResult> execute(String cmd, NotifyCallback callback);

    /**
     * Switch current instance by service
     * @param service service name
     */
    void switchService(String service);

    /**
     * Switch current instance
     * Can use {@link com.mz.jarboot.client.ServiceManagerClient#getJvmProcesses()} get the instance info or <br>
     * use {@link com.mz.jarboot.client.ServiceManagerClient#getService(String)} to get the service sid.
     * @param sid sid
     */
    void switchInstance(String sid);

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
     * shutdown executor
     */
    void shutdown();
}
