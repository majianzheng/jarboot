package com.mz.jarboot.client.command;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.client.ClientProxy;
import com.mz.jarboot.client.ServiceManagerClient;
import com.mz.jarboot.client.event.MessageRecvEvent;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.pojo.FuncRequest;
import com.mz.jarboot.common.protocol.NotifyType;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 客户端命令执行器
 * @author majianzheng
 */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class CommandExecutor implements CommandExecutorService, Subscriber<MessageRecvEvent> {
    private static final int EXEC_CMD = 0;
    private static final int CANCEL = 1;
    private final okhttp3.WebSocket client;
    private final ClientProxy proxy;
    private volatile boolean shutdown = false;
    private String sid;
    /** <sid, {@link CommandRunFuture}> */
    private final HashMap<String, CommandRunFuture> running = new HashMap<>(16);

    /**
     * CommandExecutor package private
     * @param client 客户端
     */
    CommandExecutor(ClientProxy proxy, okhttp3.WebSocket client, String sid) {
        this.client = client;
        this.proxy = proxy;
        this.sid = sid;
        NotifyReactor.getInstance().registerSubscriber(this);
    }

    @Override
    public void onEvent(MessageRecvEvent event) {
        final String id = event.getSid();
        if (StringUtils.isEmpty(id)) {
            return;
        }
        synchronized (this) {
            CommandRunFuture future = running.get(id);
            if (null == future || future.isCancelled() || future.isDone()) {
                return;
            }
            future.doCallback(event);
            if (NotifyType.COMMAND_END.equals(event.getNotifyType())) {
                future.finish(event.getSuccess(), event.getMsg());
                running.remove(id);
            }
        }
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return MessageRecvEvent.class;
    }

    @Override
    public Future<CommandResult> execute(String cmd, NotifyCallback callback) {
        check();
        CommandRunFuture future;
        boolean isOk = true;
        synchronized (this) {
            final String sidCopy = this.sid;
            try {
                future = new CommandRunFuture(cmd, callback, this::cancel);
                CommandRunFuture preFuture = null;
                if (null != (preFuture = running.putIfAbsent(sidCopy, future))) {
                    throw new JarbootRunException("Current is running command " + preFuture.cmd);
                }
                isOk = sendRequest(EXEC_CMD, sidCopy, cmd);
            } finally {
                if (!isOk) {
                    running.remove(sidCopy);
                }
            }
        }
        return future;
    }

    @Override
    public void switchService(String service) {
        check();
        synchronized (this) {
            ServiceManagerClient serviceManager = new ServiceManagerClient(proxy);
            ServiceInstance instance = serviceManager.getService(service);
            this.sid = instance.getSid();
        }
    }

    @Override
    public void switchInstance(String sid) {
        check();
        synchronized (this) {
            this.sid = sid;
        }
    }

    @Override
    public List<ServiceInstance> listServices(String filter) {
        ServiceManagerClient serviceManager = new ServiceManagerClient(proxy);
        List<ServiceInstance> services = serviceManager.getServiceList();
        if (StringUtils.isEmpty(filter) || services.isEmpty()) {
            return services;
        }
        return services
                .stream()
                .filter(inst -> StringUtils.substringMatch(inst.getName(), 0, filter) ||
                        StringUtils.substringMatch(inst.getGroup(), 0, filter) ||
                        StringUtils.substringMatch(inst.getSid(), 0, filter))
                .collect(Collectors.toList());
    }

    @Override
    public List<JvmProcess> listJvmInstances(String filter) {
        ServiceManagerClient serviceManager = new ServiceManagerClient(proxy);
        List<JvmProcess> processes = serviceManager.getJvmProcesses();
        if (StringUtils.isEmpty(filter) || processes.isEmpty()) {
            return processes;
        }
        return processes
                .stream()
                .filter(inst -> StringUtils.substringMatch(inst.getName(), 0, filter) ||
                        StringUtils.substringMatch(inst.getPid(), 0, filter) ||
                        StringUtils.substringMatch(inst.getSid(), 0, filter))
                .collect(Collectors.toList());
    }

    @Override
    public String getCurrentSid() {
        return this.sid;
    }

    @Override
    public void shutdown() {
        try {
            client.close(1000, "shutdown executor");
        } finally {
            NotifyReactor.getInstance().deregisterSubscriber(this);
            this.running.clear();
            this.shutdown = true;
        }
    }

    private boolean sendRequest(int func, String id, String cmd) {
        FuncRequest request = new FuncRequest();
        request.setBody(cmd);
        request.setFunc(func);
        request.setSid(id);
        return client.send(Objects.requireNonNull(JsonUtils.toJsonString(request)));
    }

    private boolean cancel(boolean mayInterruptIfRunning) {
        check();
        synchronized (this) {
            final String sidCopy = this.sid;
            try {
                return sendRequest(CANCEL, sidCopy, StringUtils.EMPTY);
            } finally {
                if (mayInterruptIfRunning) {
                    running.remove(sidCopy);
                }
            }
        }
    }

    private void check() {
        if (shutdown) {
            throw new JarbootRunException("Current executor is already shutdown.");
        }
    }
}
