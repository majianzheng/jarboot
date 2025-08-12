package io.github.majianzheng.jarboot;

import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.client.ClientProxy;
import io.github.majianzheng.jarboot.client.ClusterOperator;
import io.github.majianzheng.jarboot.client.ServiceManagerClient;
import io.github.majianzheng.jarboot.client.SettingClient;
import io.github.majianzheng.jarboot.client.command.CommandExecutorFactory;
import io.github.majianzheng.jarboot.client.command.CommandExecutorService;
import io.github.majianzheng.jarboot.client.command.CommandResult;
import io.github.majianzheng.jarboot.client.command.NotifyCallback;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import lombok.Getter;

import java.util.concurrent.Future;

/**
 * Jarboot客户端操作
 * @author majianzheng
 */
public class JarbootTemplate implements JarbootOperator {
    @Getter
    private final ServiceManager serviceManager;
    @Getter
    private final SettingService settingService;
    @Getter
    private final ClusterOperator clusterOperator;
    private final JarbootConfigProperties properties;
    private ClientProxy clientProxy;
    private CommandExecutorService executor;

    public JarbootTemplate(JarbootConfigProperties properties) {
        this.properties = properties;
        this.buildProxy();
        settingService = new SettingClient(this.clientProxy);
        serviceManager = new ServiceManagerClient(this.clientProxy);
        clusterOperator = new ClusterOperator(this.clientProxy);
    }

    private synchronized void buildProxy() {
        if (null == clientProxy) {
            String addr = properties.getServerAddr();
            if (StringUtils.isEmpty(addr)) {
                addr = System.getenv("JARBOOT_HOST");
            }
            if (StringUtils.isEmpty(addr)) {
                addr = "127.0.0.1:9899";
            }
            String pwd = properties.getPassword();
            if (StringUtils.isEmpty(pwd)) {
                pwd = "jarboot";
            }
            clientProxy = ClientProxy.Factory
                    .createClientProxy(
                            addr,
                            properties.getUsername(),
                            pwd);
        }
    }

    @Override
    public Future<CommandResult> execute(String serviceId, String cmd, NotifyCallback callback) {
        return executorInstance().execute(serviceId, cmd, callback);
    }

    @Override
    public void forceCancel(String serviceId) {
        executorInstance().forceCancel(serviceId);
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
