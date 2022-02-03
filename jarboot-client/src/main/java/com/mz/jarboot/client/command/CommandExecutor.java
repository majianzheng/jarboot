package com.mz.jarboot.client.command;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.client.ClientProxy;
import com.mz.jarboot.client.ServiceManagerClient;
import com.mz.jarboot.client.event.MessageRecvEvent;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.pojo.FuncRequest;
import com.mz.jarboot.common.protocol.NotifyType;
import com.mz.jarboot.common.utils.ApiStringBuilder;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
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
public class CommandExecutor implements CommandExecutorService, Subscriber<MessageRecvEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    private static final int EXEC_CMD = 0;
    private static final int CANCEL = 1;

    private okhttp3.WebSocket client;
    private final ClientProxy proxy;
    private final Subscriber<ClosedEvent> closedSub;
    private final Subscriber<PingEvent> pingSub;
    private volatile boolean shutdown = false;
    private volatile boolean online;
    private String sid;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition pingCondition = lock.newCondition();
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
        closedSub = new Subscriber<ClosedEvent>() {
            @Override
            public void onEvent(ClosedEvent event) {
                onClose();
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return ClosedEvent.class;
            }
        };
        pingSub = new Subscriber<PingEvent>() {
            @Override
            public void onEvent(PingEvent event) {
                //ping
                lock.lock();
                try {
                    pingCondition.signalAll();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return PingEvent.class;
            }
        };
        NotifyReactor.getInstance().registerSubscriber(closedSub);
        NotifyReactor.getInstance().registerSubscriber(pingSub);
        NotifyReactor.getInstance().registerSubscriber(this);
        this.online = true;
    }

    @Override
    public void onEvent(MessageRecvEvent event) {
        final String id = event.getSid();
        if (StringUtils.isEmpty(id)) {
            return;
        }
        synchronized (this) {
            CommandRunFuture future = running.get(id);
            if (null == future) {
                return;
            }
            if (future.isCancelled() || future.isDone()) {
                running.remove(id);
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
        CommandRunFuture future = null;
        synchronized (this) {
            boolean isOk = true;
            final String sidCopy = this.sid;
            try {
                future = new CommandRunFuture(cmd, callback, this::cancel);
                CommandRunFuture preFuture;
                if (null != (preFuture = running.putIfAbsent(sidCopy, future))) {
                    throw new JarbootRunException("Current is running command " + preFuture.cmd);
                }
                isOk = sendRequest(EXEC_CMD, sidCopy, cmd);
            } finally {
                if (!isOk) {
                    running.remove(sidCopy);
                    future.finish(false, "send command failed.");
                }
            }
        }
        return future;
    }

    @Override
    public void switchService(String service) {
        synchronized (this) {
            ServiceManagerClient serviceManager = new ServiceManagerClient(proxy);
            ServiceInstance instance = serviceManager.getService(service);
            this.sid = instance.getSid();
        }
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
    public boolean isOnline() {
        if (!this.online) {
            return false;
        }
        lock.lock();
        try {
            this.client.send(CommonConst.PING);
            if (!pingCondition.await(HttpRequestOperator.CONNECT_TIMEOUT, TimeUnit.SECONDS)) {
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
        if (shutdown) {
            throw new JarbootRunException("Current is already shutdown, can not reconnect.");
        }
        lock.lock();
        try {
            this.destroyClient();
            this.client = connect(proxy.getHost(), proxy.getToken());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void shutdown() {
        lock.lock();
        try {
            NotifyReactor.getInstance().deregisterSubscriber(this);
            NotifyReactor.getInstance().deregisterSubscriber(closedSub);
            NotifyReactor.getInstance().deregisterSubscriber(pingSub);
            this.running.clear();
            this.shutdown = true;
            this.destroyClient();
        } finally {
            lock.unlock();
        }
    }

    private boolean sendRequest(int func, String id, String cmd) {
        FuncRequest request = new FuncRequest();
        request.setBody(cmd);
        request.setFunc(func);
        request.setSid(id);
        return client.send(Objects.requireNonNull(JsonUtils.toJsonString(request)));
    }

    private void onClose() {
        this.online = false;
    }

    private void destroyClient() {
        try {
            client.close(1000, "shutdown executor");
        } catch (Exception e) {
            //ignore
        } finally {
            this.client = null;
        }
    }

    private boolean cancel(boolean mayInterruptIfRunning) {
        check();
        boolean success;
        lock.lock();
        try {
            final String sidCopy = this.sid;
            success = sendRequest(CANCEL, sidCopy, StringUtils.EMPTY);
            if (mayInterruptIfRunning) {
                running.remove(sidCopy);
            }
        } finally {
            lock.unlock();
        }
        return success;
    }

    private void check() {
        if (shutdown) {
            throw new JarbootRunException("Current executor is already shutdown.");
        }
        if (!this.isOnline()) {
            throw new JarbootRunException("Current executor is already offline.");
        }
        if (null == this.client) {
            throw new JarbootRunException("Client is already destroyed.");
        }
    }

    private static class ClosedEvent implements JarbootEvent {

    }
    private static class PingEvent implements JarbootEvent {

    }

    static WebSocket connect(String host, String token) {
        int index = token.indexOf(' ');
        if (-1 != index) {
            token = token.substring(index + 1);
        }
        final String url = new ApiStringBuilder(CommonConst.HTTP + host, CommonConst.MAIN_WS_CONTEXT)
                .add("accessToken", token)
                .build();
        final Request request = new Request
                .Builder()
                .get()
                .url(url)
                .build();
        CountDownLatch latch = new CountDownLatch(1);
        WebSocket client = HttpRequestOperator
                .HTTP_CLIENT
                .newWebSocket(request, new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        latch.countDown();
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        NotifyReactor
                                .getInstance()
                                .publishEvent(
                                        CommonConst.PING.equals(text) ?
                                                new CommandExecutor.PingEvent()
                                                :
                                                new MessageRecvEvent(text));
                    }

                    @Override
                    public void onClosed(WebSocket webSocket, int code, String reason) {
                        NotifyReactor.getInstance().publishEvent(new CommandExecutor.ClosedEvent());
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                        logger.warn(t.getMessage(), t);
                        NotifyReactor.getInstance().publishEvent(new CommandExecutor.ClosedEvent());
                    }
                });
        try {
            if (!latch.await(HttpRequestOperator.CONNECT_TIMEOUT, TimeUnit.SECONDS)) {
                logger.warn("Connect to jarboot server timeout! url: {}", url);
                throw new JarbootRunException("Connect to " + url + " timeout!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return client;
    }
}
