package io.github.majianzheng.jarboot.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServiceGroup;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.event.FromOtherClusterServerMessageEvent;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.event.FuncReceivedEvent;
import io.github.majianzheng.jarboot.service.impl.ServiceManagerImpl;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import io.github.majianzheng.jarboot.ws.SessionOperator;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

import javax.websocket.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 集群Api客户端
 * @author mazheng
 */
@ClientEndpoint
public class ClusterClient extends SessionOperator {
    private static final Logger logger = LoggerFactory.getLogger(ClusterClient.class);
    private static final WsWebSocketContainer CONTAINER = new WsWebSocketContainer();
    private final String serverHost;
    private final boolean clientSide;
    private boolean connecting = false;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private ConcurrentHashMap<String, RequestCallback> requestCallbackMap = new ConcurrentHashMap<>(16);

    ClusterClient(Session session, String host) {
        super(session);
        this.serverHost = host;
        clientSide = false;
    }
    ClusterClient(String host) {
        super(null);
        this.serverHost = host;
        session = connect();
        clientSide = true;
    }

    public ServiceGroup getServiceGroup() {
        ServiceGroup group = HttpUtils.getObj(formatUrl(CommonConst.CLUSTER_API_CONTEXT + "/group"), ServiceGroup.class, wrapToken());
        group.setHost(serverHost);
        return group;
    }

    public ServiceGroup getJvmGroup() {
        ServiceGroup group = HttpUtils.getObj(formatUrl(CommonConst.CLUSTER_API_CONTEXT + "/jvmGroup"), ServiceGroup.class, wrapToken());
        group.setHost(serverHost);
        return group;
    }

    public ServiceSetting getServiceSetting(String serviceName) {
        String url = formatUrl(CommonConst.CLUSTER_API_CONTEXT + "/serviceSetting?serviceName=" + serviceName);
        return HttpUtils.getObj(url, ServiceSetting.class, wrapToken());
    }

    public void deleteService(String serviceName) {
        String url = formatUrl(CommonConst.CLUSTER_API_CONTEXT + "/service?serviceName=" + serviceName);
        checkResponse(HttpUtils.delete(url, wrapToken()));
    }

    public void attach(String pid) {
        String url = formatUrl(CommonConst.CLUSTER_API_CONTEXT + "/attach?pid=" + pid);
        checkResponse(HttpUtils.get(url, wrapToken()));
    }

    public String requestSync(ClusterEventName eventName, String body, long millis) {
        String id = UUID.randomUUID().toString();
        ClusterEventMessage req = new ClusterEventMessage();
        req.setId(id);
        req.setName(eventName.name());
        req.setType(ClusterEventMessage.REQ_TYPE);
        req.setBody(body);
        req.setNeedAck(true);

        lock.lock();
        try {
            RequestCallback requestCallback = new RequestCallback();
            requestCallback.condition = lock.newCondition();
            requestCallback.id = id;
            requestCallback.reqBody = body;
            requestCallback.name = eventName.name();
            requestCallbackMap.putIfAbsent(id, requestCallback);
            sendMessage(req);
            if (requestCallback.condition.await(millis, TimeUnit.MILLISECONDS)) {
                return requestCallback.rspBody;
            } else {
                logger.error("请求{}超时,body:{}", eventName.name(), body);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            requestCallbackMap.remove(id);
            lock.unlock();
        }
        return StringUtils.EMPTY;
    }

    public boolean isAlive() {
        return null != session && session.isOpen();
    }

    public Session connect() {
        String self = ClusterClientManager.getInstance().getSelfHost();
        if (StringUtils.isEmpty(self)) {
            throw new JarbootException("cluster is not enabled, self host is empty!");
        }
        lock.lock();
        try {
            final int maxWait = 15;
            String hostEncoded = URLEncoder.encode(self, StandardCharsets.UTF_8.name());
            String url = String.format("%s%s%s/%s/%s?%s=%s",
                    CommonConst.WS,
                    serverHost,
                    CommonConst.CLUSTER_WS_CONTEXT,
                    SettingUtils.getUuid(),
                    hostEncoded,
                    AuthConst.CLUSTER_TOKEN,
                    ClusterClientManager.getInstance().getClusterToken(AuthConst.JARBOOT_USER));
            connecting = true;
            session = CONTAINER.connectToServer(this, URI.create(url));
            if (!condition.await(maxWait, TimeUnit.SECONDS)) {
                logger.error("连接集群服务{}超时！", serverHost);
            }
            return session;
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            connecting = false;
            lock.unlock();
        }
        return null;
    }

    @OnOpen
    public void onOpen(Session session) {
        lock.lock();
        try {
            this.session = session;
            if (connecting && clientSide) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }

    }
    @OnClose
    public void onClose() {
        lock.lock();
        try {
            if (connecting && clientSide) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
    @OnError
    public void onError(Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
        onClose();
    }
    @OnMessage
    public void onBinaryMessage(byte[] message) {
        final ClusterEventMessage eventMessage = JsonUtils.readValue(message, ClusterEventMessage.class);
        if (null == eventMessage) {
            logger.error("解析消息体失败！");
            return;
        }
        TaskUtils.getTaskExecutor().execute(() -> {
            String rspBody = handleEvent(eventMessage);
            if (Objects.equals(ClusterEventMessage.REQ_TYPE, eventMessage.getType()) && eventMessage.isNeedAck()) {
                // 请求内容，并且需要回执
                ClusterEventMessage resp = new ClusterEventMessage();
                resp.setId(eventMessage.getId());
                resp.setName(eventMessage.getName());
                resp.setType(ClusterEventMessage.RSP_TYPE);
                resp.setBody(rspBody);
                sendMessage(resp);
            }
            if (Objects.equals(ClusterEventMessage.RSP_TYPE, eventMessage.getType())) {
                // 响应内容
                lock.lock();
                try {
                    RequestCallback requestCallback = requestCallbackMap.get(eventMessage.getId());
                    if (null != requestCallback) {
                        requestCallback.condition.signalAll();
                        requestCallback.rspBody = rspBody;
                    }
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    void sendMessage(ClusterEventMessage message) {
        if (isAlive()) {
            newMessage(JsonUtils.toJsonBytes(message));
        }
    }

    private String handleEvent(ClusterEventMessage eventMessage) {
        ClusterEventName eventName = ClusterEventName.valueOf(eventMessage.getName());
        String resp = StringUtils.EMPTY;
        switch (eventName) {
            case NOTIFY_TO_FRONT:
                handleNotifyToFront(eventMessage);
                break;
            case EXEC_FUNC:
                execFunc(eventMessage);
                break;
            case START_SERVICE:
                resp = startLocalService(eventMessage);
                break;
            case STOP_SERVICE:
                resp = stopLocalService(eventMessage);
                break;
            default:
                logger.error("未找到处理方法：{}", eventMessage.getName());
                resp = "unknown message:" + eventMessage.getName();
                break;
        }
        return resp;
    }

    private String startLocalService(ClusterEventMessage eventMessage) {
        ServiceSetting setting = JsonUtils.readValue(eventMessage.getBody(), ServiceSetting.class);
        ServiceManagerImpl serviceManager = SettingUtils.getContext().getBean(ServiceManagerImpl.class);
        if (setting == null) {
            serviceManager.startSingleService(setting);
        } else {
            return String.format("启动服务，解析服务配置失败，setting: %s", eventMessage.getBody());
        }
        return StringUtils.EMPTY;
    }

    private String stopLocalService(ClusterEventMessage eventMessage) {
        ServiceSetting setting = JsonUtils.readValue(eventMessage.getBody(), ServiceSetting.class);
        ServiceManagerImpl serviceManager = SettingUtils.getContext().getBean(ServiceManagerImpl.class);
        if (setting == null) {
            serviceManager.stopSingleService(setting);
        } else {
            return String.format("停止服务，解析服务配置失败，setting: %s", eventMessage.getBody());
        }
        return StringUtils.EMPTY;
    }

    private static void execFunc(ClusterEventMessage eventMessage) {
        FuncReceivedEvent event = JsonUtils.readValue(eventMessage.getBody(), FuncReceivedEvent.class);
        if (null == event) {
            logger.error("序列化FuncReceivedEvent失败！{}", eventMessage.getBody());
            return;
        }
        if (StringUtils.isEmpty(event.getHost()) || StringUtils.isEmpty(event.getSessionId())) {
            logger.error("host or sessionId is empty! host:{}, sessionId:{}", event.getHost(), event.getSessionId());
            return;
        }
        String sessionId = String.format("%s %s", event.getHost(), event.getSessionId());
        event.setSessionId(sessionId);
        NotifyReactor.getInstance().publishEvent(event);
    }

    private static void handleNotifyToFront(ClusterEventMessage eventMessage) {
        FromOtherClusterServerMessageEvent event = JsonUtils
                .readValue(eventMessage.getBody(), FromOtherClusterServerMessageEvent.class);
        NotifyReactor.getInstance().publishEvent(event);
    }

    private String formatUrl(String api) {
        String url;
        final String http = "http";
        if (serverHost.startsWith(http)) {
            url = serverHost + api;
        } else {
            url = String.format("%s%s%s", CommonConst.HTTP, serverHost, api);
        }
        return url;
    }
    private Map<String, String> wrapToken() {
        String token = ClusterClientManager.getInstance().getClusterToken(SettingUtils.getCurrentLoginUsername());
        Map<String, String> header = new HashMap<>(2);
        header.put(AuthConst.CLUSTER_TOKEN, token);
        return header;
    }

    private static class RequestCallback {
        Condition condition;
        String id;
        String name;
        String reqBody;
        String rspBody;
    }

    private void checkResponse(JsonNode node) {
        ResponseSimple resp = JsonUtils.treeToValue(node, ResponseSimple.class);
        if (null == resp) {
            throw new JarbootException("请求失败！");
        }
        if (!Boolean.TRUE.equals(resp.getSuccess())) {
            throw new JarbootException(resp.getMsg());
        }
    }
}
