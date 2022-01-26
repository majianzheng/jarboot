package com.mz.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.client.event.DisconnectionEvent;
import com.mz.jarboot.client.utlis.ClientConst;
import com.mz.jarboot.client.utlis.HttpMethod;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.common.notify.NotifyReactor;
import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.common.notify.AbstractEventRegistry;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Map<String, okhttp3.WebSocket> SOCKETS = new ConcurrentHashMap<>(16);
    private static final Map<String, Set<Subscriber>> SUBS = new ConcurrentHashMap<>(16);
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

    /**
     * 请求API
     * @param api api路径
     * @param json json格式字符串
     * @param method 请求方法
     * @return response
     */
    public String reqApi(String api, String json, HttpMethod method) {
        okhttp3.Headers headers = this.authorization ? initHeader() : null;
        return HttpRequestOperator.req(this.baseUrl + api, json, headers, method);
    }

    /**
     * 请求API
     * @param api api路径
     * @param method 请求方法
     * @param requestBody 请求数据包
     * @return response
     */
    public String reqApi(String api, HttpMethod method, RequestBody requestBody) {
        okhttp3.Headers headers = this.authorization ? initHeader() : null;
        return HttpRequestOperator.req(this.baseUrl + api, method, requestBody, headers);
    }

    /**
     * 创建长连接
     * @return 长连接
     */
    private okhttp3.WebSocket newWebSocket() {
        final String url = this.baseUrl + CommonConst.EVENT_WS_CONTEXT;
        final Request request = new Request
                .Builder()
                .get()
                .url(url)
                .build();
        CountDownLatch latch = new CountDownLatch(1);
        okhttp3.WebSocket client = HttpRequestOperator
                .HTTP_CLIENT
                .newWebSocket(request, new MessageListener(this, latch));
        try {
            if (!latch.await(HttpRequestOperator.CONNECT_TIMEOUT, TimeUnit.SECONDS)) {
                logger.warn("Connect to event server timeout! url: {}", url);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return client;
    }

    /**
     * 是否token认证
     * @return 是否认证
     */
    public boolean hasAuth() {
        return this.authorization;
    }

    private okhttp3.Headers initHeader() {
        AccessToken accessToken = Factory.createToken(tokenKey, baseUrl, this.username, this.password);
        if (null == accessToken) {
            throw new JarbootRunException("request token failed.");
        }
        return new okhttp3.Headers.Builder()
                .add("Authorization", accessToken.token)
                .add("Accept", "*/*")
                .add("Content-Type", "application/json;charset=UTF-8")
                .build();
    }

    @Override
    public void registerSubscriber(String topic, Subscriber<? extends JarbootEvent> subscriber) {
        okhttp3.WebSocket socket = SOCKETS.computeIfAbsent(host, k -> this.newWebSocket());
        byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[topicBytes.length + 1];
        buf[0] = 1;
        System.arraycopy(topicBytes, 0, buf, 1, topicBytes.length);
        if (socket.send(ByteString.of(buf))) {
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
        okhttp3.WebSocket socket = SOCKETS.getOrDefault(host, null);
        if (null == socket) {
            return;
        }
        byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        byte[] buf = new byte[topicBytes.length + 1];
        System.arraycopy(topicBytes, 0, buf, 1, topicBytes.length);
        if (socket.send(ByteString.of(buf))) {
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

    private void shutdownWebSocket() {
        WebSocket socket = SOCKETS.remove(host);
        if (null == socket) {
            return;
        }
        try {
            socket.close(1000, "Connect close.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static class MessageListener extends okhttp3.WebSocketListener {
        private final String host;
        private final String username;
        private final String password;
        private final String version;
        private CountDownLatch latch;
        MessageListener(ClientProxy proxy, CountDownLatch latch) {
            this.host = proxy.host;
            this.username = proxy.username;
            this.password = proxy.password;
            this.version = proxy.version;
            this.latch = latch;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            if (null != this.latch) {
                this.latch.countDown();
                this.latch = null;
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            int i1 = bytes.indexOf(SPLIT);
            if (i1 <= 0) {
                return;
            }
            final String topic =  bytes.substring(0, i1).string(StandardCharsets.UTF_8);
            final int index = topic.indexOf(StringUtils.SLASH);
            final String className = -1 == index ? topic : topic.substring(0, index);
            ByteString bodyBytes = bytes.substring(i1 + 1);
            try {
                Class<? extends JarbootEvent> cls = (Class<? extends JarbootEvent>) Class.forName(className);
                JarbootEvent event = JsonUtils.readValue(bodyBytes.toByteArray(), cls);
                this.handler(topic, event);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            this.afterClosed();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            logger.warn(t.getMessage(), t);
            this.afterClosed();
        }

        private void afterClosed() {
            SOCKETS.remove(host);
            NotifyReactor
                    .getInstance()
                    .publishEvent(new DisconnectionEvent(this.host, this.username, this.password, this.version));
        }

        private void handler(String topic, JarbootEvent event) {
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
    }

    public static class AccessToken {
        private final String token;
        private final long expireTime;
        public AccessToken(String token, long expireTime) {
            this.token = token;
            this.expireTime = expireTime;
        }
        public boolean isExpired() {
            return System.currentTimeMillis() > this.expireTime;
        }
    }

    public static class Factory {
        static final ConcurrentHashMap<String, AccessToken> AUTH_TOKENS = new ConcurrentHashMap<>(16);

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
            AccessToken accessToken = createToken(createKey(host, user), baseUrl, user, password);
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

        /**
         * 创建token
         * @param tokenKey tokenKey
         * @param baseUrl 服务基址
         * @param username 用户名
         * @param password 密码
         * @return {@link AccessToken}
         */
        static AccessToken createToken(String tokenKey, String baseUrl, String username, String password) {
            return Factory.AUTH_TOKENS.compute(tokenKey,
                    (k, v) -> {
                        if (null == v || v.isExpired()) {
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
         * @return token {@link AccessToken}
         */
        private static AccessToken requestToken(String baseUrl, String user, String password) {
            FormBody formBody = new FormBody.Builder()
                    .add(ClientConst.USERNAME_PARAM, user)
                    .add(ClientConst.PASSWORD_PARAM, password)
                    .build();
            long current = System.currentTimeMillis();
            final String api = baseUrl + CommonConst.AUTH_CONTEXT + "/login";
            String body = HttpRequestOperator.req(api, HttpMethod.POST, formBody, null);
            JsonNode jsonNode = JsonUtils.readAsJsonNode(body);
            if (null == jsonNode) {
                throw new JarbootRunException("Request token failed!" + body);
            }
            int resultCode =jsonNode.get(ClientConst.RESULT_CODE_KEY).asInt(-1);
            if (ResultCodeConst.SUCCESS != resultCode) {
                JsonNode resultMsgNode = jsonNode.get(ClientConst.RESULT_MSG_KEY);
                String msg = null == resultMsgNode ? StringUtils.EMPTY : resultMsgNode.asText(StringUtils.EMPTY);
                String resultMsg = String.format("Request token failed! %s, user:%s, password:%s",
                        msg, user, password);
                throw new JarbootRunException(resultMsg);
            }
            JsonNode resultNode = jsonNode.get(ClientConst.RESULT_KEY);
            String token = resultNode.get(ClientConst.ACCESS_TOKEN_KEY).asText(StringUtils.EMPTY);
            long ttl = resultNode.get(ClientConst.ACCESS_TTL_KEY).asLong(-1);
            if (StringUtils.isEmpty(token) || -1 == ttl) {
                throw new JarbootRunException("Request token is empty!");
            }
            return new AccessToken("Bearer " + token, current + ttl);
        }

        /**
         * 获取Jarboot服务版本
         * @param baseUrl Jarboot服务基址
         * @return 版本
         */
        private static String getVersion(String baseUrl) {
            final String api = baseUrl + CommonConst.CLOUD_CONTEXT + "/version";
            return HttpRequestOperator.req(api, StringUtils.EMPTY, null, HttpMethod.GET);
        }

        static String createKey(String host, String username) {
            return host + StringUtils.LF + username;
        }

        private Factory() {}
    }
}
