package io.github.majianzheng.jarboot.cluster;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServiceGroup;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * 集群Api客户端
 * @author mazheng
 */
@ClientEndpoint
public class ClusterClient {
    private static final Logger logger = LoggerFactory.getLogger(ClusterClient.class);
    private Session session;
    private String serverHost;
    private boolean alive = false;
    private Map<String, CountDownLatch> req;

    public ServiceGroup getServiceGroup() {
        ServiceGroup group = HttpUtils.getObj(formatUrl(CommonConst.CLUSTER_CONTEXT + "/group"), ServiceGroup.class, wrapToken());
        group.setHost(serverHost);
        return group;
    }

    public ServiceGroup getJvmGroup() {
        ServiceGroup group = HttpUtils.getObj(formatUrl(CommonConst.CLUSTER_CONTEXT + "/jvmGroup"), ServiceGroup.class, wrapToken());
        group.setHost(serverHost);
        return group;
    }

    public void request(String name, String body) {

    }

    public boolean isAlive() {
        return alive;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.alive = true;
    }
    @OnClose
    public void onClose() {
        this.alive = false;
    }
    @OnError
    public void onError(Throwable throwable) {
        this.alive = false;
        logger.error(throwable.getMessage(), throwable);
    }
    @OnMessage
    public void onBinaryMessage(byte[] message) {
        ClusterEventMessage eventMessage = JsonUtils.readValue(message, ClusterEventMessage.class);
        if (null == eventMessage) {
            logger.error("解析消息体失败！");
            return;
        }
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
        }
    }

    private void sendMessage(ClusterEventMessage message) {
        if (!alive) {
            return;
        }
        ByteBuffer data = ByteBuffer.wrap(JsonUtils.toJsonBytes(message));
        try {
            session.getBasicRemote().sendBinary(data);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String handleEvent(ClusterEventMessage eventMessage) {
        return StringUtils.EMPTY;
    }

    private String formatUrl(String api) {
        String url;
        final String http = "http";
        if (serverHost.startsWith(http)) {
            url = serverHost + api;
        } else {
            url = String.format("%s://%s%s", http, serverHost, api);
        }
        return url;
    }
    private Map<String, String> wrapToken() {
        String token = ClusterConfig.getInstance().getClusterToken(SettingUtils.getCurrentLoginUsername());
        Map<String, String> header = new HashMap<>(2);
        header.put(AuthConst.CLUSTER_TOKEN, token);
        return header;
    }
}
