package io.github.majianzheng.jarboot.core.basic;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.protocol.CommandRequest;
import io.github.majianzheng.jarboot.common.protocol.CommandResponse;
import io.github.majianzheng.jarboot.common.protocol.ResponseType;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.cmd.CommandRequestSubscriber;
import io.github.majianzheng.jarboot.core.cmd.CommandSubscriber;
import io.github.majianzheng.jarboot.core.cmd.InternalCommandSubscriber;
import io.github.majianzheng.jarboot.core.event.HeartbeatEvent;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.slf4j.Logger;

import javax.websocket.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket client factory for create socket client.
 * @author majianzheng
 */
@SuppressWarnings("squid:S3077")
@ClientEndpoint
public class WsClientFactory implements Subscriber<HeartbeatEvent> {
    private static final Logger logger = LogUtils.getLogger();

    /** 最大连接等待时间默认10秒 */
    private static final int MAX_CONNECT_WAIT_SECOND = 10;
    /** 心跳间隔，仅远程连接时 */
    private static final int HEARTBEAT_INTERVAL = 15;
    /** 重连的间隔时间，加上连接等待时间10秒，一共每隔15秒执行一次尝试连接 */
    private static final int RECONNECT_INTERVAL = 8;
    /** WebSocket 客户端 */
    private final WsWebSocketContainer container = new WsWebSocketContainer();
    private Session session;
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
    private final AtomicBoolean shutdown  = new AtomicBoolean(false);
    private final AtomicBoolean destroyingClient  = new AtomicBoolean(false);
    /** 心跳Future */
    private ScheduledFuture<?> heartbeatFuture = null;
    private Thread reconnectThread = null;

    private WsClientFactory() {
        //注册事件订阅
        NotifyReactor.getInstance().registerSubscriber(new CommandRequestSubscriber());
        NotifyReactor.getInstance().registerSubscriber(new CommandSubscriber());
        NotifyReactor.getInstance().registerSubscriber(new InternalCommandSubscriber());
        NotifyReactor.getInstance().registerSubscriber(this);
        NotifyReactor.getInstance().addShutdownCallback(this::shutdown);
    }

    public void countDown() {
        if (null != latch) {
            latch.countDown();
        } else {
            LogUtils.offlineDevLog("latch is null!");
        }
    }

    @OnOpen
    public void onOpen() {
        LogUtils.offlineDevLog("websocket open!");
        online = true;
    }

    @OnMessage
    public void onMessage(byte[] message) {
        CommandRequest request = new CommandRequest();
        request.fromRaw(message);
        NotifyReactor.getInstance().publishEvent(request);
    }

    @OnClose
    public void onClosed(Session session) {
        LogUtils.offlineDevLog("onClosed {}", session.getId());
        onClose();
    }

    @OnError
    public void onFailure(Throwable t) {
        LogUtils.offlineDevLog("连接异常");
        if (!destroyingClient.get()) {
            logger.error("连接异常：{}", t.getMessage(), t);
        }
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
     * 发送数据
     * @param data 数据
     */
    public void send(byte[] data) {
        Session temp = this.session;
        if (null == temp) {
            return;
        }
        try {
            temp.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 创建客户端
     */
    public synchronized void createSingletonClient() {
        LogUtils.offlineDevLog(">> online: {}, connection:{}, shutdown:{}", online, connecting, shutdown.get());
        if (online || connecting || shutdown.get()) {
            return;
        }
        if (null != session) {
            this.destroyClient();
        }
        String serviceName = null;
        String sid = null;
        String userDir = null;
        try {
            serviceName = URLEncoder.encode(EnvironmentContext.getAgentClient().getServiceName(), StandardCharsets.UTF_8.name());
            sid = URLEncoder.encode(EnvironmentContext.getAgentClient().getSid(), StandardCharsets.UTF_8.name());
            userDir = URLEncoder.encode(EnvironmentContext.getAgentClient().getUserDir(), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return;
        }
        String host = EnvironmentContext.getAgentClient().getHost();
        int index = host.indexOf("//");
        if (index > 0 ) {
            host = host.substring(index + 2);
        }
        final String url = new StringBuilder()
                .append(CommonConst.WS)
                .append(host)
                .append(CommonConst.AGENT_WS_CONTEXT)
                .append(StringUtils.SLASH)
                .append(serviceName)
                .append(StringUtils.SLASH)
                .append(sid)
                .append(StringUtils.SLASH)
                .append(userDir)
                .toString();
        LogUtils.offlineDevLog("connecting to jarboot {}", url);
        latch = new CountDownLatch(1);
        try {
            connecting = true;
            // 使用Jarboot类加载器
            Thread.currentThread().setContextClassLoader(WsClientFactory.class.getClassLoader());
            session = container.connectToServer(this, URI.create(url));
            if (latch.await(MAX_CONNECT_WAIT_SECOND, TimeUnit.SECONDS) || online) {
                LogUtils.offlineDevLog("start scheduleHeartbeat");
                scheduleHeartbeat();
            } else {
                LogUtils.offlineDevLog("wait connect timeout.");
                this.destroyClient();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogUtils.offlineDevLog("wait connect interrupted.");
            this.destroyClient();
        } catch (Throwable e) {
            if (reconnectNotStarted) {
                // 重连时忽略异常报错
                logger.error(e.getMessage(), e);
            }
            this.destroyClient();
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
            LogUtils.offlineDevLog("Client is not online can't start schedule heartbeat.");
            return;
        }
        if (null == heartbeatFuture) {
            //启动监控，断开时每隔一段时间尝试重连
            reconnectEnabled = true;
            LogUtils.offlineDevLog("心跳任务启动...");
            //每隔一段时间进行一次心跳探测
             heartbeatFuture = EnvironmentContext
                .getScheduledExecutor()
                .scheduleWithFixedDelay(this::sendHeartbeat, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
        } else {
            LogUtils.offlineDevLog("心跳已经启动！");
        }
    }

    /**
     * 是否在线
     * @return 是否在线
     */
    public boolean checkOnline() {
        if (online) {
            LogUtils.offlineDevLog("check on line");
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
        LogUtils.offlineDevLog("change host: {}", host);
        //修改host
        System.setProperty(CommonConst.REMOTE_PROP, host);
        EnvironmentContext.getAgentClient().setHost(host);
        closeSession();
        createSingletonClient();
    }

    /**
     * 主动关闭会话
     */
    public void closeSession() {
        LogUtils.offlineDevLog("主动关闭");
        ScheduledFuture<?> future = this.heartbeatFuture;
        if (null != future) {
            future.cancel(true);
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
        if (null == this.session || connecting || !reconnectNotStarted || shutdown.get()) {
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
            send(raw);
            // 等待jarboot-server的心跳命令触发
            online = heartbeatLatch.await(MAX_CONNECT_WAIT_SECOND, TimeUnit.SECONDS);
            if (!online) {
                LogUtils.offlineDevLog("wait heartbeat callback timeout!");
            }
        } catch (InterruptedException e) {
            LogUtils.offlineDevLog("sendHeartbeat interrupted!");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LogUtils.offlineDevLog("Check online send heartbeat failed. {}", e.getMessage());
            online = false;
        } finally {
            this.heartbeatLatch = null;
            if (!online) {
                LogUtils.offlineDevLog("sendHeartbeat failed destroy client!");
                this.destroyClient();
            }
        }
    }

    private synchronized void onClose() {
        online = false;
        if (destroyingClient.compareAndSet(true, false)) {
            LogUtils.offlineDevLog("正在销毁websocket客户端");
        } else {
            Session temp = this.session;
            if (null != temp) {
                try {
                    temp.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            this.session = null;
        }
        EnvironmentContext.cleanSession();
        if (null == shutdownLatch) {
            // 非主动关闭的异常断开
            if (reconnectEnabled && reconnectNotStarted && null == reconnectThread) {
                // 防止重复创建线程
                reconnectNotStarted = false;
                reconnectThread = JarbootThreadFactory
                        .createThreadFactory("reconnect-task", true)
                        .newThread(this::reconnect);
                reconnectThread.start();
            }
        } else {
            // 主动关闭连接
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
                    LogUtils.offlineDevLog("reconnect to jarboot...");
                    createSingletonClient();
                }
            }
        } catch (InterruptedException e) {
            //允许外部中断
            LogUtils.offlineDevLog("reconnect thread is interrupted");
            Thread.currentThread().interrupt();
        } finally {
            reconnectNotStarted = true;
            reconnectThread = null;
        }
    }

    private void destroyClient() {
        LogUtils.offlineDevLog(">>>destroy client");
        if (destroyingClient.compareAndSet(false, true)) {
            if (null == this.session) {
                return;
            }
            try {
                Thread thread = this.reconnectThread;
                if (null != thread) {
                    thread.interrupt();
                }
            } catch (Exception e) {
                // ignore
            }
            try {
                session.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            session = null;
        }
    }

    private void shutdown() {
        LogUtils.offlineDevLog(">>>>shutdown triggered.");
        if (shutdown.compareAndSet(false, true)) {
            ScheduledFuture<?> future = this.heartbeatFuture;
            if (null != future) {
                future.cancel(true);
                heartbeatFuture = null;
            }
            Thread thread = this.reconnectThread;
            if (null != thread) {
                thread.interrupt();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.destroyClient();
        }
    }

    /**
     * Jarboot服务发送的心跳事件
     */
    @Override
    public void onEvent(HeartbeatEvent event) {
        CountDownLatch heartLatch = this.heartbeatLatch;
        if (null != heartLatch) {
            heartLatch.countDown();
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
