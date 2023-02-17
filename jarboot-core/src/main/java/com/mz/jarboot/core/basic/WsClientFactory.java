package com.mz.jarboot.core.basic;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.common.*;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.protocol.CommandRequest;
import com.mz.jarboot.common.protocol.CommandResponse;
import com.mz.jarboot.common.protocol.ResponseType;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.core.cmd.CommandRequestSubscriber;
import com.mz.jarboot.core.cmd.CommandSubscriber;
import com.mz.jarboot.core.cmd.InternalCommandSubscriber;
import com.mz.jarboot.core.event.HeartbeatEvent;
import com.mz.jarboot.core.utils.HttpUtils;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.core.utils.ThreadUtil;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client factory for create socket client.
 * @author majianzheng
 */
@SuppressWarnings("squid:S3077")
public class WsClientFactory extends WebSocketListener implements Subscriber<HeartbeatEvent> {
    private static final Logger logger = LogUtils.getLogger();

    /** 最大连接等待时间默认10秒 */
    private static final int MAX_CONNECT_WAIT_SECOND = 10;
    /** 心跳间隔，仅远程连接时 */
    private static final int HEARTBEAT_INTERVAL = 15;
    /** 重连的间隔时间，加上连接等待时间10秒，一共每隔15秒执行一次尝试连接 */
    private static final int RECONNECT_INTERVAL = 5;
    /** WebSocket 客户端 */
    private okhttp3.WebSocket client = null;
    /** 是否在线标志 */
    private volatile boolean online = false;
    /** 连接等待latch */
    private volatile CountDownLatch latch = null;
    /** 心跳latch */
    private volatile CountDownLatch heartbeatLatch = null;
    /** 销毁连接latch */
    private volatile CountDownLatch shutdownLatch = null;
    /** 是否启动重连 */
    private volatile boolean reconnectEnabled = false;
    /** 是否正在连接 */
    private boolean connecting = false;
    /** 重连未开始标志 */
    private boolean reconnectNotStarted = true;
    /** 心跳Future */
    private ScheduledFuture<?> heartbeatFuture = null;

    private WsClientFactory() {
        //注册事件订阅
        NotifyReactor.getInstance().registerSubscriber(new CommandRequestSubscriber());
        NotifyReactor.getInstance().registerSubscriber(new CommandSubscriber());
        NotifyReactor.getInstance().registerSubscriber(new InternalCommandSubscriber());
        NotifyReactor.getInstance().registerSubscriber(this);
    }


    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        online = true;
        if (null != latch) {
            latch.countDown();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        CommandRequest request = new CommandRequest();
        request.fromRaw(bytes.toByteArray());
        NotifyReactor.getInstance().publishEvent(request);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        onClose();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        onClose();
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
        if (online || connecting) {
            return;
        }
        this.destroyClient();
        final String url = new StringBuilder()
                .append(CommonConst.WS)
                .append(EnvironmentContext.getAgentClient().getHost())
                .append(CommonConst.AGENT_WS_CONTEXT)
                .append(StringUtils.SLASH)
                .append(EnvironmentContext.getAgentClient().getServiceName())
                .append(StringUtils.SLASH)
                .append(EnvironmentContext.getAgentClient().getSid())
                .toString();
        AnsiLog.info("connecting to jarboot {}", url);
        latch = new CountDownLatch(1);
        try {
            connecting = true;
            client = HttpUtils.HTTP_CLIENT
                    .newWebSocket(new Request
                            .Builder()
                            .get()
                            .url(url)
                            .build(), this);
            if (latch.await(MAX_CONNECT_WAIT_SECOND, TimeUnit.SECONDS)) {
                scheduleHeartbeat();
            } else {
                logger.warn("wait connect timeout.");
                this.destroyClient();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            latch = null;
            connecting = false;
        }
    }

    /**
     * 连接间隔心跳探测
     */
    public void scheduleHeartbeat() {
        if (!online) {
            AnsiLog.error("Client is not online can't start schedule heartbeat.");
            return;
        }
        //启动监控，断开时每隔一段时间尝试重连
        AnsiLog.info("init client success! reconnect enabled, start heartbeat.");
        reconnectEnabled = true;
        //每隔一段时间进行一次心跳探测
        heartbeatFuture = EnvironmentContext
                .getScheduledExecutor()
                .scheduleWithFixedDelay(this::sendHeartbeat, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * 是否在线
     * @return 是否在线
     */
    public boolean checkOnline() {
        if (online) {
            sendHeartbeat();
        }
        return online;
    }

    public boolean isOnline() {
        return this.online;
    }

    /**
     * Jarboot服务地址更新
     * @param host jarboot服务地址，eg: 192.168.1.88:9899
     */
    public void changeHost(String host) {
        //修改host
        System.setProperty(CommonConst.REMOTE_PROP, host);
        EnvironmentContext.getAgentClient().setHost(host);
        HttpUtils.setBaseUrl(CommonConst.HTTP + host);
        closeSession();
        createSingletonClient();
    }

    /**
     * 主动关闭会话
     */
    public void closeSession() {
        if (null != heartbeatFuture) {
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }
        if (online) {
            // 销毁旧的连接
            shutdownLatch = new CountDownLatch(1);
            try {
                this.destroyClient();
                if (!shutdownLatch.await(RECONNECT_INTERVAL, TimeUnit.SECONDS)) {
                    logger.warn("wait destroy timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                shutdownLatch = null;
            }
        }
    }

    private void sendHeartbeat() {
        if (null == this.client || connecting || !reconnectNotStarted) {
            return;
        }
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.HEARTBEAT);
        resp.setBody("heartbeat time:" + System.currentTimeMillis());
        heartbeatLatch = new CountDownLatch(1);
        try {
            // 进行一次心跳检测
            byte[] raw = resp.toRaw();
            online = this.client.send(ByteString.of(raw, 0, raw.length));
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
        // 检测线程，若只剩下最后一个非守护线程则退出
        List<Thread> threads = ThreadUtil.getThreadList();
        boolean exist = false;
        int count = 0;
        boolean alive = false;
        final String name = "DestroyJavaVM";
        for (Thread thread : threads) {
            if (!thread.isDaemon()) {
                if (thread.getName().equals(name)) {
                    exist = true;
                } else {
                    ++count;
                }
                if (thread.getName().startsWith("OkHttp WebSocket")) {
                    alive = true;
                }
            }
        }
        if (exist && alive && count == 1) {
            this.destroyClient();
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
                if (!connecting && !online) {
                    createSingletonClient();
                }
            }
        } catch (InterruptedException e) {
            //允许外部中断
            Thread.currentThread().interrupt();
        } finally {
            reconnectNotStarted = true;
        }
    }

    private void destroyClient() {
        if (null == this.client) {
            return;
        }
        try {
            client.close(1000, "Connect close.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        client = null;
    }

    /**
     * Jarboot服务发送的心跳事件
     */
    @Override
    public void onEvent(HeartbeatEvent event) {
        if (null != this.heartbeatLatch) {
            this.heartbeatLatch.countDown();
        }
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return HeartbeatEvent.class;
    }

    /**
     * 单例模式，内部私有类方式
     */
    private static class WsClientFactoryHolder {
        static final WsClientFactory INSTANCE = new WsClientFactory();
    }
}
