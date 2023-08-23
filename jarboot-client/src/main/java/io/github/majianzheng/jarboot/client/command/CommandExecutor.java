package io.github.majianzheng.jarboot.client.command;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.JvmProcess;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.client.ClientProxy;
import io.github.majianzheng.jarboot.client.ServiceManagerClient;
import io.github.majianzheng.jarboot.client.event.MessageRecvEvent;
import io.github.majianzheng.jarboot.common.pojo.FuncRequest;
import io.github.majianzheng.jarboot.common.protocol.NotifyType;
import io.github.majianzheng.jarboot.common.utils.ApiStringBuilder;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 客户端命令执行器
 * @author majianzheng
 */
@SuppressWarnings({"PMD.ServiceOrDaoClassShouldEndWithImplRule", "java:S2274"})
@ClientEndpoint
public class CommandExecutor implements CommandExecutorService, MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    private static final int EXEC_CMD = 0;
    private static final int CANCEL = 1;
    private static final int WAIT_TIME = 15;

    Session client;
    private final ClientProxy proxy;
    private volatile boolean online;
    private String sid;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition pingCondition = lock.newCondition();
    /** <sid, {@link CommandRunFuture}> */
    private final ConcurrentHashMap<String, CommandRunFuture> running = new ConcurrentHashMap<>(16);

    /**
     * CommandExecutor package private
     */
    CommandExecutor(ClientProxy proxy, String sid) {
        this.proxy = proxy;
        this.sid = sid;
        this.online = true;
    }

    @Override
    public Future<CommandResult> execute(String cmd, NotifyCallback callback) {
        final String serviceId = this.sid;
        if (StringUtils.isEmpty(serviceId)) {
            throw new JarbootRunException("sid is empty!");
        }
        return execute(serviceId, cmd, callback);
    }

    @Override
    public Future<CommandResult> execute(String serviceId, String cmd, NotifyCallback callback) {
        check();
        CommandRunFuture future = new CommandRunFuture(serviceId, cmd, callback, this::cancel);
        CommandRunFuture preFuture;
        if (null != (preFuture = running.putIfAbsent(serviceId, future))) {
            throw new JarbootRunException("Current is running command " + preFuture.cmd);
        }
        if (!sendRequest(EXEC_CMD, serviceId, cmd)) {
            running.remove(serviceId);
            future.finish(false, "send command failed.");
        }

        return future;
    }

    @Override
    public void forceCancel(String serviceId) {
        CommandRunFuture future = running.get(serviceId);
        if (null == future) {
            sendRequest(CANCEL, serviceId, StringUtils.EMPTY);
        } else {
            future.cancel(true);
        }
    }

    @Override
    public void switchService(String service) {
        ServiceManagerClient serviceManager = new ServiceManagerClient(proxy);
        ServiceInstance instance = serviceManager.getService(service);
        this.switchInstance(instance.getSid());
    }

    @Override
    public void switchInstance(String sid) {
        lock.lock();
        try {
            this.sid = sid;
        } finally {
            lock.unlock();
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
    public boolean checkOnline() {
        lock.lock();
        try {
            this.send(CommonConst.PING);
            if (!pingCondition.await(WAIT_TIME, TimeUnit.SECONDS)) {
                this.online = false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return this.online;
    }

    @Override
    public void tryReconnect() {
        lock.lock();
        try {
            this.destroyClient();
            this.client = connect(proxy.getHost(), proxy.getToken(), this);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void shutdown() {
        lock.lock();
        try {
            this.running.clear();
            this.destroyClient();
            this.online = false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @OnOpen
    public void onOpen() {
        this.online = true;
        synchronized (this) {
            this.notify();
        }
    }

    @Override
    @OnMessage
    public void onMessage(String text) {
        if (CommonConst.PING.equals(text)) {
            //ping
            lock.lock();
            try {
                pingCondition.signalAll();
            } finally {
                lock.unlock();
            }
            return;
        }
        this.onEvent(new MessageRecvEvent(text));
    }

    @Override
    @OnClose
    public void onClose() {
        this.online = false;
        synchronized (this) {
            this.notify();
        }
    }

    private boolean sendRequest(int func, String id, String cmd) {
        try {
            FuncRequest request = new FuncRequest();
            request.setBody(cmd);
            request.setFunc(func);
            request.setSid(id);
            return this.send(Objects.requireNonNull(JsonUtils.toJsonString(request)));
        } catch (Exception e) {
            logger.error("send failed. " + e.getMessage(), e);
        }
        return false;
    }

    private void onEvent(MessageRecvEvent event) {
        final String serviceId = event.getSid();
        if (StringUtils.isEmpty(serviceId)) {
            return;
        }
        CommandRunFuture future = running.get(serviceId);
        if (null == future) {
            return;
        }
        if (future.isCancelled() || future.isDone()) {
            running.remove(serviceId);
            return;
        }
        future.doCallback(event);
        if (NotifyType.COMMAND_END.equals(event.getNotifyType())) {
            future.finish(event.getSuccess(), event.getMsg());
            running.remove(serviceId);
        }
    }

    private boolean send(String data) {
        Session temp = this.client;
        if (null == temp) {
            return false;
        }
        try {
            temp.getBasicRemote().sendText(data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private void destroyClient() {
        try {
            if (null != client) {
                client.close();
            }
        } catch (Exception e) {
            //ignore
        } finally {
            this.client = null;
        }
    }

    private boolean cancel(String serviceId, boolean mayInterruptIfRunning) {
        boolean success = sendRequest(CANCEL, serviceId, StringUtils.EMPTY);
        if (mayInterruptIfRunning) {
            running.remove(serviceId);
        }
        return success;
    }

    private void check() {
        if (null == this.client) {
            throw new JarbootRunException("Client is already destroyed.");
        }
        if (!this.online) {
            throw new JarbootRunException("Current executor is already offline.");
        }
    }

    static Session connect(String host, String token, final MessageListener listener) {
        int index = token.indexOf(' ');
        if (-1 != index) {
            token = token.substring(index + 1);
        }
        final String url = new ApiStringBuilder(CommonConst.WS + host, CommonConst.MAIN_WS_CONTEXT)
                .add("accessToken", token)
                .build();
        Session client = ClientProxy.connectToServer(listener, url);
        try {
            synchronized (listener) {
                listener.wait(WAIT_TIME * 1000);
            }
            if (!client.isOpen()) {
                logger.warn("Connect to jarboot server timeout! url: {}", url);
                throw new JarbootRunException("Connect to " + url + " timeout!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return client;
    }
}
