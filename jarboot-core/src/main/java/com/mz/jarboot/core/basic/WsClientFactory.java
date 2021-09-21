package com.mz.jarboot.core.basic;

import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
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
    private static final int MAX_CONNECT_WAIT_SECOND = 10;
    private okhttp3.WebSocket client = null;
    private String url = null;
    private final CommandDispatcher dispatcher;
    private okhttp3.WebSocketListener listener;
    private volatile boolean online = false;
    @SuppressWarnings("all")
    private volatile CountDownLatch latch = null;
    @SuppressWarnings("all")
    private volatile CountDownLatch heartbeatLatch = null;

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
                if (null != latch) {
                    latch.countDown();
                }
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
        if (null != client && online) {
            try {
                client.close(0, "destroy and recreate");
            } catch (Exception e) {
                //ignore
            }
        }
        latch = new CountDownLatch(1);
        try {
            client = HttpUtils.HTTP_CLIENT
                    .newWebSocket(new Request
                            .Builder()
                            .get()
                            .url(url)
                            .build(), this.listener);
            
            long b = System.currentTimeMillis();
            logger.debug("wait connected:{}", b);
            boolean r = latch.await(MAX_CONNECT_WAIT_SECOND, TimeUnit.SECONDS);
            if (r) {
                logger.debug("wait time:{}", System.currentTimeMillis() - b);
            } else {
                logger.warn("wait connect timeout.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            latch = null;
        }
    }

    public void onHeartbeat() {
        if (null == this.heartbeatLatch) {
            logger.warn("heartbeat command executed, but check thread may timeout!");
        } else {
            logger.info("heartbeat command executed success!");
            this.heartbeatLatch.countDown();
        }
    }

    public boolean isOnline() {
        if (online) {
            sendHeartbeat();
        }
        return online;
    }

    private void sendHeartbeat() {
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.HEARTBEAT);
        resp.setBody("heartbeat time:" + System.currentTimeMillis());
        resp.setSessionId(CommandConst.SESSION_COMMON);
        heartbeatLatch = new CountDownLatch(1);
        try {
            // 进行一次心跳检测
            online = this.client.send(resp.toRaw());
            logger.info("check online send heartbeat >> success: {}", online);
            if (!online) {
                // 发送心跳失败！
                return;
            }
            // 等待jarboot-server的心跳命令触发
            online = heartbeatLatch.await(MAX_CONNECT_WAIT_SECOND, TimeUnit.SECONDS);
            if (online) {
                logger.info("wait heartbeat callback success!");
            } else {
                logger.error("wait heartbeat callback timeout!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.heartbeatLatch = null;
        }
    }

    public okhttp3.WebSocket getSingletonClient() {
        return this.client;
    }
}
