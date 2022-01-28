package com.mz.jarboot.client.command;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.client.ClientProxy;
import com.mz.jarboot.client.ServiceManagerClient;
import com.mz.jarboot.client.event.MessageRecvEvent;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.utils.ApiStringBuilder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 客户端命令执行器工厂类
 * @author majianzheng
 */
public class CommandExecutorFactory {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutorFactory.class);
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
     * @param sid sid
     * @return {@link CommandExecutorService}
     */
    public static CommandExecutorService createCommandExecutor(ClientProxy proxy, String sid) {
        if (null == proxy) {
            throw new JarbootRunException("Create client proxy is null!");
        }
        WebSocket client = connect(proxy.getHost(), proxy.getToken());
        return new CommandExecutor(proxy, client, sid);
    }

    private static WebSocket connect(String host, String token) {
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
                        NotifyReactor.getInstance().publishEvent(new MessageRecvEvent(text));
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

    private CommandExecutorFactory() {}
}
