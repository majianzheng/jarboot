package io.github.majianzheng.jarboot.cluster;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author mazheng
 */
@ServerEndpoint(CommonConst.CLUSTER_WS_CONTEXT + "/{uuid}/{host}")
@RestController
public class ClusterEventServer {
    private static final Logger logger = LoggerFactory.getLogger(ClusterEventServer.class);
    @OnOpen
    public void onOpen(Session session, @PathParam("host") String host, @PathParam("uuid") String uuid) {
        ClusterClientManager.getInstance().addClient(session, host, uuid);
    }

    @OnClose
    public void onClose( Session session, @PathParam("host") String host) {
        ClusterClient client = ClusterClientManager.getInstance().getClient(host);
        if (null != client) {
            client.onClose();
        }
    }

    @OnMessage
    public void onBinaryMessage(byte[] message, Session session, @PathParam("host") String host) {
        ClusterClient client = ClusterClientManager.getInstance().getClient(host);
        if (null == client) {
            logger.error("client is null");
        } else {
            client.onBinaryMessage(message);
        }
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("host") String host) {
        ClusterClient client = ClusterClientManager.getInstance().getClient(host);
        if (null != client) {
            client.onError(error);
        }
    }
}
