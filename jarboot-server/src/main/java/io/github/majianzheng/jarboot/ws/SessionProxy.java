package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * WebSocket代理
 * @author mazheng
 */
@ClientEndpoint
public class SessionProxy {
    private static final Logger logger = LoggerFactory.getLogger(SessionProxy.class);
    private final Session client;
    private final Session targetSession;
    public SessionProxy(Session client, String targetClusterHost) {
        this.client = client;
        URI clientUri = client.getRequestURI();
        int index = targetClusterHost.indexOf(':');
        String host = targetClusterHost.substring(0, index);
        String query = clientUri.getQuery();
        query = parseQuery(client, query);
        int port = Integer.parseInt(targetClusterHost.substring(index + 1));
        try {
            URI uri = new URI(clientUri.getScheme(),
                    clientUri.getUserInfo(),
                    host, port,
                    clientUri.getPath(),
                    query,
                    clientUri.getFragment());
            targetSession = client.getContainer().connectToServer(this, uri);
        } catch (Exception e) {
            throw new JarbootException(String.format("连接[%s]失败，msg: %s", e.getMessage(), targetClusterHost), e);
        }
    }

    private String parseQuery(Session session, String query) {
        // 从Session的UserProperties中获取Cookie
        String accessClusterHost = (String) session.getUserProperties().get(AuthConst.ACCESS_CLUSTER_HOST);
        String token = (String) session.getUserProperties().get(AuthConst.ACCESS_TOKEN);
        final String prefix = StringUtils.isEmpty(query) ? StringUtils.EMPTY : "&";
        if (StringUtils.isNotEmpty(token) && !query.contains(AuthConst.ACCESS_TOKEN)) {
            query += (prefix + AuthConst.ACCESS_TOKEN + "=" + token);
        }
        if (StringUtils.isNotEmpty(accessClusterHost) && !query.contains(AuthConst.ACCESS_CLUSTER_HOST)) {
            query += (prefix + AuthConst.ACCESS_CLUSTER_HOST + "=" + accessClusterHost);
        }
        return query;
    }

    public void proxyOnText(String message) {
        try {
            targetSession.getBasicRemote().sendText(message);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void proxyBinary(byte[] buf) {
        try {
            targetSession.getBasicRemote().sendBinary(ByteBuffer.wrap(buf));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void proxyOnClose() {
        try {
            targetSession.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @OnMessage
    public void onMessage(byte[] message) {
        try {
            client.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    @OnMessage
    public void onTextMessage(String message) {
        try {
            client.getBasicRemote().sendText(message);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    @OnOpen
    public void onOpen() {
        logger.info("代理Session连接成功！");
    }
    @OnClose
    public void onClosed() {
        logger.info("代理Session连接关闭！");
        closeClient();
    }
    @OnError
    public void onFailure(Throwable t) {
        logger.error("代理Session连接异常，{}", t.getMessage(), t);
        closeClient();
    }
    private void closeClient() {
        try {
            client.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
