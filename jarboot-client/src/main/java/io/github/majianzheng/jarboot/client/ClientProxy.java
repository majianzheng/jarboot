package io.github.majianzheng.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.client.event.DisconnectionEvent;
import io.github.majianzheng.jarboot.client.utlis.ClientConst;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.notify.AbstractEventRegistry;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 客户端请求代理
 * @author majianzheng
 */
@SuppressWarnings({"unused", "java:S3740", "unchecked", "rawtypes"})
public class ClientProxy implements AbstractEventRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ClientProxy.class);
    private static final Map<String, WsClient> SOCKETS = new ConcurrentHashMap<>(16);
    private static final Map<String, Set<Subscriber>> SUBS = new ConcurrentHashMap<>(16);
    private static final WsWebSocketContainer CONTAINER = new WsWebSocketContainer();
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String host;
    private final boolean authorization;
    private String tokenKey;
    private final String version;

    private ClientProxy(String host, String username, String password, String version) {
        this.baseUrl = CommonConst.HTTP + host;
        this.host = host;
        this.username = username;
        this.password = password;
        this.version = version;
        if (null == username) {
            this.authorization = false;
        } else {
            tokenKey = Factory.createKey(host, username);
            this.authorization = true;
        }
    }

    private ClientProxy(String host, String version) {
        this(host, null, null, version);
    }

    public String getHost() {
        return this.host;
    }

    public String getVersion() {
        return this.version;
    }

    public static Session connectToServer(Object obj, String url) {
        try {
            return CONTAINER.connectToServer(obj, URI.create(url));
        } catch (Exception e) {
            throw new JarbootRunException(e);
        }
    }

    /**
     * 请求API
     * @param api api路径
     * @return response
     */
    public JsonNode get(String api) {
        return HttpUtils.get(this.baseUrl + api, initHeader());
    }

    public JsonNode postJson(String api, Object obj) {
        return HttpUtils.postJson(this.baseUrl + api, obj, initHeader());
    }

    public JsonNode postForm(String api, Map<String, String> form) {
        return HttpUtils.post(this.baseUrl + api, form, initHeader());
    }

    public JsonNode delete(String api) {
        return HttpUtils.delete(this.baseUrl + api, initHeader());
    }


    @ClientEndpoint
    public static class WsClient {
        private Session session;
        private String host;
        private String username;
        private CountDownLatch latch;

        public boolean send(byte[] data) {
            Session temp = this.session;
            if (null == temp) {
                return false;
            }
            try {
                temp.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return false;
            }
            return true;
        }
        public void close() {
            Session temp = this.session;
            if (null == temp) {
                return;
            }
            try {
                temp.close();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        @OnOpen
        public void onOpen() {
            CountDownLatch temp = this.latch;
            if (null != temp) {
                temp.countDown();
            }
        }
        @OnMessage
        public void onMessage(byte[] message) {
            recvMessage(message);
        }

        @OnClose
        public void onClosed() {
            SOCKETS.remove(host);
            NotifyReactor
                    .getInstance()
                    .publishEvent(new DisconnectionEvent(host, username));
        }

        @OnError
        public void onFailure(Throwable t) {
            logger.error("连接异常：{}", t.getMessage(), t);
            onClosed();
        }
        public WsClient(String host, String username, String accessToken) {
            this.host = host;
            this.username = username;
            final String url = CommonConst.WS + host + CommonConst.EVENT_WS_CONTEXT + "?accessToken=" + accessToken;
            latch = new CountDownLatch(1);
            try {
                final int maxWait = 5;
                session = connectToServer(this, url);
                if (!latch.await(maxWait, TimeUnit.SECONDS)) {
                    logger.warn("Connect to event server timeout! url: {}", url);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                throw new JarbootRunException(e);
            } finally {
                latch = null;
            }
        }
    }

    /**
     * 是否token认证
     * @return 是否认证
     */
    public boolean hasAuth() {
        return this.authorization;
    }

    public String getToken() {
        String accessToken = Factory.createToken(tokenKey, baseUrl, this.username, this.password);
        if (null == accessToken) {
            throw new JarbootRunException("request token failed.");
        }
        return accessToken;
    }

    private Map<String, String> initHeader() {
        String token = getToken();
        HashMap<String, String> header = new HashMap<>(8);
        header.put("Authorization", token);
        header.put("Accept", "*/*");
        header.put("Content-Type", "application/json;charset=UTF-8");
        return header;
    }

    @Override
    public void registerSubscriber(String topic, Subscriber<? extends JarbootEvent> subscriber) {
        WsClient socket = SOCKETS.computeIfAbsent(host, k -> new WsClient(host, username, getToken()));
        byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[topicBytes.length + 1];
        buf[0] = 1;
        System.arraycopy(topicBytes, 0, buf, 1, topicBytes.length);
        if (socket.send(buf)) {
            SUBS.compute(topic, (k, v) -> {
                //订阅计数
                if (null == v) {
                    v = new HashSet<>(16);
                }
                v.add(subscriber);
                return v;
            });
        } else {
            logger.warn("Send to event server failed when registerSubscriber.{}", topic);
            throw new JarbootRunException("send data error, register subscriber failed.");
        }
    }

    @Override
    public void deregisterSubscriber(String topic, Subscriber<? extends JarbootEvent> subscriber) {
        WsClient socket = SOCKETS.getOrDefault(host, null);
        if (null == socket) {
            return;
        }
        byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[topicBytes.length + 1];
        System.arraycopy(topicBytes, 0, buf, 1, topicBytes.length);
        if (socket.send(buf)) {
            SUBS.computeIfPresent(topic, (k, v) -> {
                v.remove(subscriber);
                if (v.isEmpty()) {
                    v = null;
                    if (SUBS.size() <= 1) {
                        logger.debug("topics will be zero shutdown client.");
                        //当前没有任何订阅销毁WebSocket
                        this.shutdownWebSocket();
                    }
                }
                return v;
            });
        } else {
            logger.warn("Send to event server failed when deregisterSubscriber.{}", topic);
            throw new JarbootRunException("send data error, deregister subscriber failed.");
        }
    }

    @Override
    public void receiveEvent(String topic, JarbootEvent event) {
        //ignore
    }

    private static void recvMessage(byte[] bytes) {
        int i1 = -1;
        for (int i = 0; i < bytes.length; ++i) {
            if (bytes[i] == SPLIT[0]) {
                i1 = i;
                break;
            }
        }
        if (i1 <= 0) {
            return;
        }
        final String topic =  new String(bytes, 0, i1, StandardCharsets.UTF_8);
        final int index = topic.indexOf(StringUtils.SLASH);
        final String className = -1 == index ? topic : topic.substring(0, index);
        try {
            Class<? extends JarbootEvent> cls = (Class<? extends JarbootEvent>) Class.forName(className);
            JarbootEvent event = JsonUtils.readValue(new String(bytes, i1 + 1, bytes.length - i1 - 1, StandardCharsets.UTF_8), cls);
            handler(topic, event);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void handler(String topic, JarbootEvent event) {
        Set<Subscriber> subs = SUBS.getOrDefault(topic, null);
        if (null != subs && !subs.isEmpty()) {
            subs.forEach(sub -> {
                Executor executor = sub.executor();
                //执行本地事件
                final Runnable job = () -> sub.onEvent(event);
                if (null == executor) {
                    job.run();
                } else {
                    executor.execute(job);
                }
            });
        }
    }

    private void shutdownWebSocket() {
        WsClient socket = SOCKETS.remove(host);
        if (null == socket) {
            return;
        }
        try {
            socket.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static class Factory {
        static final ConcurrentHashMap<String, String> AUTH_TOKENS = new ConcurrentHashMap<>(16);

        private static final ConcurrentHashMap<String, HashMap<String, ClientProxy>> CLIENTS =
                new ConcurrentHashMap<>(16);

        /**
         * 创建客户端代理
         * @param host jarboot服务地址
         * @param user jarboot用户名
         * @param password jarboot用户密码
         * @return 客户端代理 {@link ClientProxy}
         */
        public static ClientProxy createClientProxy(final String host, final String user, final String password) {
            final String baseUrl = CommonConst.HTTP + host;
            String accessToken = createToken(createKey(host, user), baseUrl, user, password);
            if (null == accessToken) {
                throw new JarbootRunException("create token failed.");
            }
            String version = getVersion(baseUrl);
            if (StringUtils.isEmpty(version)) {
                throw new JarbootRunException("Get jarboot server version failed.");
            }
            return CLIENTS.compute(host, (k, v) -> {
                if (null == v) {
                    v = new HashMap<>(4);
                }
                v.computeIfAbsent(user, k1 -> new ClientProxy(host, user, password, version));
                return v;
            }).get(user);
        }

        /**
         * 创建客户端代理
         * @param host jarboot服务地址
         * @return 客户端代理 {@link ClientProxy}
         */
        public static ClientProxy createClientProxy(final String host) {
            final String baseUrl = CommonConst.HTTP + host;
            String version = getVersion(baseUrl);
            if (StringUtils.isEmpty(version)) {
                throw new JarbootRunException("Get jarboot server version failed.");
            }
            HashMap<String, ClientProxy> map = CLIENTS.computeIfAbsent(host, k -> {
                HashMap<String, ClientProxy> userClientMap = new HashMap<>(4);
                userClientMap.put(StringUtils.EMPTY, new ClientProxy(host, version));
                return userClientMap;
            });
            return map.values().iterator().next();
        }

        public static void destroyClientProxy(final ClientProxy proxy) {
            CLIENTS.computeIfPresent(proxy.host, (k, v) -> {
                v.remove(proxy.username);
                if (v.isEmpty()) {
                    v = null;
                    //销毁socket
                    WsClient socket = SOCKETS.remove(proxy.host);
                    if (null != socket) {
                        try {
                            socket.close();
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
                return v;
            });
        }

        /**
         * 创建token
         * @param tokenKey tokenKey
         * @param baseUrl 服务基址
         * @param username 用户名
         * @param password 密码
         * @return token
         */
        static String createToken(String tokenKey, String baseUrl, String username, String password) {
            return Factory.AUTH_TOKENS.compute(tokenKey,
                    (k, v) -> {
                        if (null == v) {
                            try {
                                return requestToken(baseUrl, username, password);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        return v;
                    });
        }

        /**
         * 获取token
         * @param baseUrl 基址
         * @param user 用户名
         * @param password 用户密码
         * @return token
         */
        private static String requestToken(String baseUrl, String user, String password) {
            Map<String, String> formData = new HashMap<>(8);
            formData.put(ClientConst.USERNAME_PARAM, user);
            formData.put(ClientConst.PASSWORD_PARAM, password);

            long current = System.currentTimeMillis();
            final String api = baseUrl + CommonConst.AUTH_CONTEXT + "/openApiToken";
            JsonNode jsonNode = HttpUtils.post(api, formData, null);
            if (null == jsonNode) {
                throw new JarbootRunException("Request token failed!" + user);
            }
            int resultCode =jsonNode.get(ClientConst.RESULT_CODE_KEY).asInt(-1);
            if (ResultCodeConst.SUCCESS != resultCode) {
                JsonNode resultMsgNode = jsonNode.get(ClientConst.RESULT_MSG_KEY);
                String msg = null == resultMsgNode ? StringUtils.EMPTY : resultMsgNode.asText(StringUtils.EMPTY);
                String resultMsg = String.format("Request token failed! %s, user:%s, password:%s",
                        msg, user, password);
                throw new JarbootRunException(resultMsg);
            }
            String token = jsonNode.get(ClientConst.RESULT_KEY).asText();
            if (StringUtils.isEmpty(token)) {
                throw new JarbootRunException("Request token is empty!");
            }
            return token;
        }

        /**
         * 获取Jarboot服务版本
         * @param baseUrl Jarboot服务基址
         * @return 版本
         */
        private static String getVersion(String baseUrl) {
            final String api = baseUrl + CommonConst.SERVER_RUNTIME_CONTEXT;
            return HttpUtils.getObj(api, ServerRuntimeInfo.class, null).getVersion();
        }

        static String createKey(String host, String username) {
            return host + StringUtils.LF + username;
        }

        private Factory() {}
    }
}
