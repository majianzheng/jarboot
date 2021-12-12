package com.mz.jarboot.core.basic;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.*;
import com.mz.jarboot.core.cmd.CommandDispatcher;
import com.mz.jarboot.core.utils.HttpUtils;
import com.mz.jarboot.core.utils.LogUtils;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client factory for create socket client.
 * @author majianzheng
 */
@SuppressWarnings("all")
public class WsClientFactory {
    private static final Logger logger = LogUtils.getLogger();

    /** 最大连接等待时间默认10秒 */
    private static final int MAX_CONNECT_WAIT_SECOND = 10;
    /** 心跳间隔，仅远程连接时 */
    private static final int HEARTBEAT_INTERVAL = 15;
    /** 重连的间隔时间，加上连接等待时间10秒，一共每隔15秒执行一次尝试连接 */
    private static final int RECONNECT_INTERVAL = 5;
    /** 获取连接的IP地址 */
    private static final String API_REMOTE_ADDR = "/api/jarboot/public/agent/remoteAddr";
    /** WebSocket 客户端 */
    private okhttp3.WebSocket client = null;
    /** 命令派发器 */
    private final CommandDispatcher dispatcher;
    /** WebSocket事件监听 */
    private okhttp3.WebSocketListener listener;
    /** 是否在线标志 */
    private volatile boolean online = false;
    /** 连接等待latch */
    private volatile CountDownLatch latch = null;
    /** 心跳latch */
    private volatile CountDownLatch heartbeatLatch = null;
    /** 销毁连接latch */
    private volatile CountDownLatch shutdownLatch = null;
    /** 是否启动重连 */
    private boolean reconnectEnabled = false;
    /** 是否正在连接 */
    private boolean connectting = false;
    /** 重连未开始标志 */
    private boolean reconnectNotStarted = true;

    private WsClientFactory() {
        //1.命令派发器
        dispatcher = new CommandDispatcher(this::onHeartbeat);
        //2.初始化WebSocket的handler
        this.initMessageHandler();
    }

    private void initMessageHandler() {
        this.listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
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
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                onClose();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                onClose();
            }
        };
    }

    /**
     * 获取单例
     * @return 单例
     */
    public static WsClientFactory getInstance() {
        return WsClientFactoryHolder.INSTANCE;
    }

    /**
     * 获取客户端
     * @return 客户端
     */
    public okhttp3.WebSocket getSingletonClient() {
        return this.client;
    }

    /**
     * 创建客户端
     */
    public synchronized void createSingletonClient() {
        if (online) {
            return;
        }
        if (null != client) {
            this.destroyClient();
        }
        final String url = String.format("ws://%s/jarboot/public/agent/ws/%s/%s",
                EnvironmentContext.getClientData().getHost(),
                EnvironmentContext.getClientData().getServer(),
                EnvironmentContext.getClientData().getSid());
        latch = new CountDownLatch(1);
        try {
            connectting = true;
            client = HttpUtils.HTTP_CLIENT
                    .newWebSocket(new Request
                            .Builder()
                            .get()
                            .url(url)
                            .build(), this.listener);
            if (!latch.await(MAX_CONNECT_WAIT_SECOND, TimeUnit.SECONDS)) {
                logger.warn("wait connect timeout.");
                this.destroyClient();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            latch = null;
            connectting = false;
        }
    }

    public static String getRemoteAddr() {
        ResponseForObject resp = HttpUtils
                .postJson(API_REMOTE_ADDR, "", ResponseForObject.class);
        if (null == resp) {
            logger.warn("remoteJvm request failed.");
            return null;
        }
        if (ResultCodeConst.SUCCESS != resp.getResultCode()) {
            logger.warn("remoteJvm request failed. {}", resp.getResultMsg());
            return null;
        }
        Object result = resp.getResult();
        if (result instanceof String) {
            return (String)result;
        }
        return null;
    }

    /**
     * 远程连接间隔心跳探测
     */
    public void remoteJvmSchedule() {
        //启动监控，断开时每隔一段时间尝试重连
        logger.info("remote jvm connect success! reconnect enabled.");
        reconnectEnabled = true;
        //每隔一段时间进行一次心跳探测
        EnvironmentContext
                .getScheduledExecutorService()
                .scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Jarboot服务发送的心跳事件
     */
    public void onHeartbeat() {
        if (null != this.heartbeatLatch) {
            this.heartbeatLatch.countDown();
        }
    }

    /**
     * 是否在线
     * @return 是否在线
     */
    public boolean isOnline() {
        if (online) {
            sendHeartbeat();
        }
        return online;
    }

    public void changeHost(String host) {
        //修改host
        System.setProperty(CommonConst.REMOTE_PROP, host);
        EnvironmentContext.getClientData().setHost(host);
        HttpUtils.setHost(host);
        if (online) {
            // 销毁旧的连接
            shutdownLatch = new CountDownLatch(1);
            try {
                this.destroyClient();
                shutdownLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                shutdownLatch = null;
            }
        }
        createSingletonClient();
    }

    private void sendHeartbeat() {
        if (null == this.client || connectting || !reconnectNotStarted) {
            return;
        }
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.HEARTBEAT);
        resp.setBody("heartbeat time:" + System.currentTimeMillis());
        resp.setSessionId(CommandConst.SESSION_COMMON);
        heartbeatLatch = new CountDownLatch(1);
        try {
            // 进行一次心跳检测
            online = this.client.send(resp.toRaw());
            if (!online) {
                // 发送心跳失败！
                logger.warn("Check online send heartbeat failed.");
                return;
            }
            // 等待jarboot-server的心跳命令触发
            online = heartbeatLatch.await(MAX_CONNECT_WAIT_SECOND, TimeUnit.SECONDS);
            if (!online) {
                logger.error("wait heartbeat callback timeout!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.heartbeatLatch = null;
            if (!online) {
                this.destroyClient();
            }
        }
    }

    private synchronized void onClose() {
        online = false;
        this.destroyClient();
        EnvironmentContext.cleanSession();
        if (null == shutdownLatch) {
            //非主动关闭的异常断开
            if (reconnectEnabled && reconnectNotStarted) {
                //防止重复创建线程
                reconnectNotStarted = false;
                JarbootThreadFactory
                        .createThreadFactory("reconnect-task", true)
                        .newThread(this::reconnect)
                        .start();
            }
        } else {
            //主动关闭连接
            shutdownLatch.countDown();
        }
    }

    /**
     * 重连
     */
    private synchronized void reconnect() {
        //断开重连
        try {
            for (; ; ) {
                if (online) {
                    logger.info("reconnect success!");
                    break;
                }
                TimeUnit.SECONDS.sleep(RECONNECT_INTERVAL);
                if (!connectting && !online) {
                    createSingletonClient();
                }
            }
        } catch (InterruptedException e) {
            //允许外部中断
            Thread.currentThread().interrupt();
        } finally {
            reconnectNotStarted = true;
            logger.info("reconnect<");
        }
    }

    private void destroyClient() {
        if (!online) {
            return;
        }
        if (null == this.client) {
            return;
        }
        try {
            client.cancel();
        } catch (Exception e) {
            //ignore
        }
        try {
            client.close(1100, "Connect close.");
        } catch (Exception e) {
            //ignore
        }
        client = null;
    }

    /**
     * 单例模式，内部私有类方式
     */
    private static class WsClientFactoryHolder {
        static final WsClientFactory INSTANCE = new WsClientFactory();
    }
}
