package io.github.majianzheng.jarboot.config;

import io.github.majianzheng.jarboot.constant.AuthConst;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

/**
 * websocket配置器
 * @author majianzheng
 */
public class WsConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        Map<String, List<String>> headers = request.getHeaders();
        List<String> cookies = headers.get("Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                parseCookie(sec, cookie);
            }
        }
        super.modifyHandshake(sec, request, response);
    }

    private static void parseCookie(ServerEndpointConfig sec, String cookie) {
        final String tokenPrefix = "jt_token=";
        final String clusterPrefix = "jt_cluster=";
        int tokenBegin = cookie.indexOf(tokenPrefix);
        if (tokenBegin >= 0) {
            int tokenEnd = cookie.indexOf(";", tokenBegin);
            if (tokenEnd < 0) {
                tokenEnd = cookie.length();
            }
            String token = cookie.substring(tokenBegin + tokenPrefix.length(), tokenEnd);
            sec.getUserProperties().put(AuthConst.ACCESS_TOKEN, token);
        }
        int clusterBegin = cookie.indexOf(clusterPrefix);
        if (clusterBegin >= 0) {
            int clusterEnd = cookie.indexOf(";", clusterBegin);
            if (clusterEnd < 0) {
                clusterEnd = cookie.length();
            }
            String clusterHost = cookie.substring(clusterBegin + clusterPrefix.length(), clusterEnd);
            sec.getUserProperties().put(AuthConst.ACCESS_CLUSTER_HOST, clusterHost);
        }
    }
}
