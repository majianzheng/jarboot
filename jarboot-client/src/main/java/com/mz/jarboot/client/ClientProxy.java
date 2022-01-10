package com.mz.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.client.utlis.ClientConst;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author majianzheng
 */
public class ClientProxy {
    private final String baseUrl;
    private final String user;
    private final String password;
    private final String host;
    private final boolean authorization;

    private ClientProxy(String host, String user, String password) {
        this.baseUrl = CommonConst.HTTP + host;
        this.host = host;
        this.user = user;
        this.password = password;
        this.authorization = true;
    }

    private ClientProxy(String host) {
        this.baseUrl = CommonConst.HTTP + host;
        this.host = host;
        this.user = null;
        this.password = null;
        this.authorization = false;
    }

    public String reqApi(String api, String json, HttpRequestOperator.HttpMethod method) {
        Map<String, String> headers = this.authorization ? initHeader() : null;
        return HttpRequestOperator.reqJson(this.baseUrl + api, json, headers, method);
    }

    public String reqApi(String api, Map<String, String> form, HttpRequestOperator.HttpMethod method) {
        Map<String, String> headers = this.authorization ? initHeader() : null;
        return HttpRequestOperator.req(this.baseUrl + api, form, headers, method);
    }

    public boolean hasAuth() {
        return this.authorization;
    }

    private Map<String, String> initHeader() {
        Map<String, String> headers = new HashMap<>(4);
        AccessToken accessToken = Factory.AUTH_TOKENS.compute(this.createTokenKey(),
                (k, v) -> {
            if (null == v || v.isExpired()) {
                return Factory.requestToken(this.baseUrl, this.user, this.password);
            }
            return v;
        });
        headers.put("Authorization", accessToken.token);
        headers.put("Accept", "*/*");
        headers.put("Content-Type", "application/json;charset=UTF-8");
        return headers;
    }

    private String createTokenKey() {
        return this.host + StringUtils.LF + this.user;
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

        public String getToken() {
            return this.token;
        }
    }

    public static class Factory {
        static final ConcurrentHashMap<String, AccessToken> AUTH_TOKENS = new ConcurrentHashMap<>(16);

        private static final ConcurrentHashMap<String, HashMap<String, ClientProxy>> CLIENTS =
                new ConcurrentHashMap<>(16);

        public static ClientProxy createClientProxy(final String host, final String user, final String password) {
            return CLIENTS.computeIfAbsent(host, k -> {
                HashMap<String, ClientProxy> userClientMap = new HashMap<>(4);
                userClientMap.put(user, new ClientProxy(host, user, password));
                return userClientMap;
            }).get(user);
        }

        public static ClientProxy createClientProxy(final String host) {
            HashMap<String, ClientProxy> map = CLIENTS.computeIfAbsent(host, k -> {
                HashMap<String, ClientProxy> userClientMap = new HashMap<>(4);
                userClientMap.put(StringUtils.EMPTY, new ClientProxy(host));
                return userClientMap;
            });
            return map.values().iterator().next();
        }

        static AccessToken requestToken(String baseUrl, String user, String password) {
            HashMap<String, String> param = new HashMap<>(4);
            param.put("username", user);
            param.put("password", password);
            long current = System.currentTimeMillis();
            final String api = baseUrl + CommonConst.AUTH_CONTEXT + "/login";
            String body = HttpRequestOperator.req(api, param, null, HttpRequestOperator.HttpMethod.POST);
            JsonNode jsonNode = JsonUtils.readAsJsonNode(body);
            if (null == jsonNode) {
                throw new JarbootRunException("Request token failed!" + body);
            }
            int resultCode =jsonNode.get(ClientConst.RESULT_CODE_KEY).asInt(-1);
            if (ResultCodeConst.SUCCESS != resultCode) {
                String resultMsg = String.format("Request token failed! %s, user:%s, password:%s",
                        jsonNode.get(ClientConst.RESULT_MSG_KEY).asText(StringUtils.EMPTY), user, password);
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

        private Factory() {}
    }
}
