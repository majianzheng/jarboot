package io.github.majianzheng.jarboot;

import io.github.majianzheng.jarboot.api.constant.TaskLifecycle;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.api.pojo.SystemSetting;
import io.github.majianzheng.jarboot.api.pojo.JvmProcess;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.client.ClientProxy;
import io.github.majianzheng.jarboot.client.ServiceManagerClient;
import io.github.majianzheng.jarboot.client.SettingClient;
import io.github.majianzheng.jarboot.client.command.CommandExecutorFactory;
import io.github.majianzheng.jarboot.client.command.CommandExecutorService;
import io.github.majianzheng.jarboot.client.command.CommandResult;
import io.github.majianzheng.jarboot.client.command.NotifyCallback;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Jarboot客户端操作
 * @author majianzheng
 */
public class JarbootTemplate implements JarbootOperator {
    private final ServiceManager serviceManager;
    private final SettingService settingService;
    private final JarbootConfigProperties properties;
    private ClientProxy clientProxy;
    private CommandExecutorService executor;

    public JarbootTemplate(JarbootConfigProperties properties) {
        this.properties = properties;
        this.buildProxy();
        settingService = new SettingClient(this.clientProxy);
        serviceManager = new ServiceManagerClient(this.clientProxy);
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public SettingService getSettingService() {
        return settingService;
    }

    private synchronized void buildProxy() {
        if (null == clientProxy) {
            String addr = properties.getServerAddr();
            if (StringUtils.isEmpty(addr)) {
                addr = "127.0.0.1:9899";
            }
            clientProxy = ClientProxy.Factory
                    .createClientProxy(
                            addr,
                            properties.getUsername(),
                            properties.getPassword());
        }
    }

    @Override
    public String getServiceIdByName(String service) {
        return serviceManager.getService(service).getSid();
    }

    @Override
    public ServiceInstance getService(String serviceName) {
        return serviceManager.getService(serviceName);
    }

    @Override
    public List<JvmProcess> getJvmProcesses() {
        return serviceManager.getJvmProcesses();
    }

    @Override
    public Future<CommandResult> execute(String serviceId, String cmd, NotifyCallback callback) {
        return executorInstance().execute(serviceId, cmd, callback);
    }

    @Override
    public void forceCancel(String serviceId) {
        executorInstance().forceCancel(serviceId);
    }

    @Override
    public ServiceSetting getServiceSetting(String serviceName) {
        return settingService.getServiceSetting(serviceName);
    }

    @Override
    public SystemSetting getGlobalSetting() {
        return settingService.getSystemSetting();
    }

    @Override
    public void registerTaskLifecycleSubscriber(String serviceName, TaskLifecycle lifecycle, Subscriber<TaskLifecycleEvent> subscriber) {
        serviceManager.registerSubscriber(serviceName, lifecycle, subscriber);
    }

    @Override
    public void deregisterTaskLifecycleSubscriber(String serviceName, TaskLifecycle lifecycle, Subscriber<TaskLifecycleEvent> subscriber) {
        serviceManager.deregisterSubscriber(serviceName, lifecycle, subscriber);
    }

    public CommandExecutorService executorInstance() {
        CommandExecutorService local = executor;
        if (null == local) {
            synchronized (this) {
                local = executor;
                if (null == local) {
                    executor = local = CommandExecutorFactory
                            .createCommandExecutor(this.clientProxy, StringUtils.EMPTY);
                }
            }
        }
        return local;
    }
}
