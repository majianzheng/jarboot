package com.mz.jarboot;

import com.mz.jarboot.api.service.ServiceManager;
import com.mz.jarboot.api.service.SettingService;
import com.mz.jarboot.client.ClientProxy;
import com.mz.jarboot.client.ServiceManagerClient;
import com.mz.jarboot.client.SettingClient;

/**
 * Jarboot客户端操作
 * @author majianzheng
 */
public class JarbootTemplate {
    private ServiceManager serviceManager;
    private SettingService settingService;
    private final JarbootConfigProperties properties;
    private ClientProxy clientProxy;

    public JarbootTemplate(JarbootConfigProperties properties) {
        this.properties = properties;
        this.buildProxy();
    }

    public ServiceManager getServiceManager() {
        if (null == serviceManager) {
            serviceManager = new ServiceManagerClient(this.clientProxy);
        }
        return serviceManager;
    }

    public SettingService getSettingService() {
        if (null == settingService) {
            settingService = new SettingClient(this.clientProxy);
        }
        return settingService;
    }

    private synchronized void buildProxy() {
        if (null == clientProxy) {
            clientProxy = ClientProxy.Factory
                    .createClientProxy(
                            properties.getServerAddr(),
                            properties.getUsername(),
                            properties.getPassword());
        }
    }
}
