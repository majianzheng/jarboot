package com.mz.jarboot.core.basic;

import com.mz.jarboot.core.cmd.CommandDispatcher;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.HttpUtils;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client factory for create socket client.
 * @author majianzheng
 */
public class WsClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    @SuppressWarnings("all")
    private static volatile WsClientFactory instance = null;
    private okhttp3.WebSocket client = null;
    private String url = null;
    private final CommandDispatcher dispatcher;
    private okhttp3.WebSocketListener listener;
    private volatile boolean online = false;
    private CountDownLatch latch = new CountDownLatch(1);

    private WsClientFactory() {
        //1.命令派发器
        dispatcher = new CommandDispatcher();
        //2.初始化WebSocket的handler
        this.initMessageHandler();

        String server = EnvironmentContext.getServer();
        //服务目录名支持中文，检查到中文后进行编码
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]").matcher(server);
        while (matcher.find()) {
            String tmp = matcher.group();
            try {
                server = server.replaceAll(tmp, java.net.URLEncoder.encode(tmp, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }

        url = String.format("ws://%s/public/jarboot/agent/ws/%s",
                EnvironmentContext.getHost(), server);
        logger.debug("initClient {}", url);
    }

    private void initMessageHandler() {
        this.listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.debug("client connected>>>");
                online = true;
                latch.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                dispatcher.publish(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                dispatcher.publish(bytes.string(StandardCharsets.UTF_8));
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                online = false;
                EnvironmentContext.cleanSession();
                logger.debug("onClosing>>>{}", reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                online = false;
                logger.debug("onClosed>>>{}", reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.error("onFailure>>>", t);
                online = false;
            }
        };
    }

    public static WsClientFactory getInstance() {
        if (null == instance) {
            synchronized (WsClientFactory.class) {
                if (null == instance) {
                    instance = new WsClientFactory();
                }
            }
        }
        return instance;
    }

    public synchronized void createSingletonClient() {
        try {
            client = HttpUtils.HTTP_CLIENT
                    .newWebSocket(new Request
                            .Builder()
                            .get()
                            .url(url)
                            .build(), this.listener);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        try {
            long b = System.currentTimeMillis();
            logger.debug("wait connected:{}", b);
            boolean r = latch.await(5, TimeUnit.SECONDS);
            if (r) {
                logger.debug("wait time:{}", System.currentTimeMillis() - b);
            } else {
                logger.warn("wait connect timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            //重置供下次使用
            latch = new CountDownLatch(1);
        }
    }

    public boolean isOnline() {
        return online;
    }

    public okhttp3.WebSocket getSingletonClient() {
        return this.client;
    }
}
