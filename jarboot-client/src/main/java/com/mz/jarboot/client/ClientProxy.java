package com.mz.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.client.utlis.ClientConst;
import com.mz.jarboot.client.utlis.HttpMethod;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端请求代理
 * @author majianzheng
 */
public class ClientProxy {
    private static final Logger logger = LoggerFactory.getLogger(ClientProxy.class);
    private final String baseUrl;
    private final String user;
    private final String password;
    private final String host;
    private final boolean authorization;
    private String tokenKey = StringUtils.EMPTY;
    private final String version;

    private ClientProxy(String host, String user, String password, String version) {
        this.baseUrl = CommonConst.HTTP + host;
        this.host = host;
        this.user = user;
        this.password = password;
        this.authorization = true;
        this.version = version;
        tokenKey = Factory.createTokenKey(host, user);
    }

    private ClientProxy(String host, String version) {
        this.baseUrl = CommonConst.HTTP + host;
        this.host = host;
        this.user = null;
        this.password = null;
        this.authorization = false;
        this.version = version;
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
        Map<String, String> headers = this.authorization ? initHeader() : null;
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
        Map<String, String> headers = this.authorization ? initHeader() : null;
        return HttpRequestOperator.req(this.baseUrl + api, method, requestBody, headers);
    }

    /**
     * 是否token认证
     * @return 是否认证
     */
    public boolean hasAuth() {
        return this.authorization;
    }

    private Map<String, String> initHeader() {
        Map<String, String> headers = new HashMap<>(4);
        AccessToken accessToken = Factory.createToken(tokenKey, baseUrl, this.user, this.password);
        if (null == accessToken) {
            throw new JarbootRunException("request token failed.");
        }
        headers.put("Authorization", accessToken.token);
        headers.put("Accept", "*/*");
        headers.put("Content-Type", "application/json;charset=UTF-8");
        return headers;
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
            AccessToken accessToken = createToken(createTokenKey(host, user), baseUrl, user, password);
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

        static String createTokenKey(String host, String username) {
            return host + StringUtils.LF + username;
        }

        private Factory() {}
    }
}
