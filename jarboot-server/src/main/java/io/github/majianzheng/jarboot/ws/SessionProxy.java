package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.utils.CommonUtils;
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
        String accessClusterHost = CommonUtils.getSessionParam(AuthConst.ACCESS_CLUSTER_HOST, client);
        String query = clientUri.getQuery();
        if (StringUtils.isEmpty(accessClusterHost)) {
            query += String.format("&%s=%s", AuthConst.ACCESS_CLUSTER_HOST, ClusterClientManager.getInstance().getSelfHost());
        }
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
            throw new JarbootException(e);
        }
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
            // ignore
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
