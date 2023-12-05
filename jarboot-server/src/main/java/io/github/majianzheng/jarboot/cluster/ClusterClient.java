package io.github.majianzheng.jarboot.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.*;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.event.FromOtherClusterServerMessageEvent;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.HttpUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.event.FuncReceivedEvent;
import io.github.majianzheng.jarboot.security.JwtTokenManager;
import io.github.majianzheng.jarboot.service.impl.ServiceManagerImpl;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 集群Api客户端
 * @author mazheng
 */
public class ClusterClient {
    private static final Logger logger = LoggerFactory.getLogger(ClusterClient.class);
    private final String host;
    private final String name;
    private ClusterServerState state;
    /** 是否是主节点 */
    private boolean master;
    private final ConcurrentHashMap<String, RequestCallback> requestCallbackMap = new ConcurrentHashMap<>(16);

    ClusterClient(String host) {
        int index = host.indexOf(' ');
        if (index > 0) {
            this.host = host.substring(0, index).trim();
            this.name = host.substring(index + 1).trim();
        } else {
            this.host = host;
            this.name = host;
        }
        this.state = ClusterServerState.OFFLINE;
    }

    public ClusterServerState getState() {
        return state;
    }

    public void setState(ClusterServerState state) {
        this.state = state;
    }

    public boolean isOnline() {
        return ClusterServerState.ONLINE.equals(state);
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public ServiceInstance getServiceGroup() {
        ServiceInstance group = HttpUtils.getObj(formatUrl("/group"), ServiceInstance.class, wrapToken());
        group.setHost(host);
        group.setHostName(name);
        return group;
    }

    public JvmProcess getJvmGroup() {
        JvmProcess group = HttpUtils.getObj(formatUrl("/jvmGroup"), JvmProcess.class, wrapToken());
        group.setHost(host);
        group.setHostName(name);
        return group;
    }

    public ServiceSetting getServiceSetting(String serviceName) {
        String url = formatUrl("/serviceSetting?serviceName=" + serviceName);
        return HttpUtils.getObj(url, ServiceSetting.class, wrapToken());
    }

    public void saveServiceSetting(ServiceSetting setting) {
        String url = formatUrl("/serviceSetting");
        ResponseSimple resp = HttpUtils.postObj(url, setting, ResponseSimple.class, wrapToken());
        if (!resp.getSuccess()) {
            throw new JarbootException(resp.getCode(), resp.getMsg());
        }
    }

    public void deleteService(String serviceName) {
        String url = formatUrl("/service?serviceName=" + serviceName);
        checkResponse(HttpUtils.delete(url, wrapToken()));
    }

    public void attach(String pid) {
        String url = formatUrl("/attach?pid=" + pid);
        checkResponse(HttpUtils.get(url, wrapToken()));
    }

    public void upload(String filename, String path, InputStream is) {
        String url = formatUrl("/file");
        Map<String, String> params = new HashMap<>(2);
        params.put("path", path);
        HttpUtils.upload(url, is, filename, params, wrapToken());
    }

    public void download(String path, OutputStream os) {
        String url = formatUrl("/file/download");
        Map<String, String> params = new HashMap<>(2);
        params.put("path", path);
        HttpUtils.post(url, os, params, wrapToken());
    }

    public List<FileNode> fileList(String baseDir, boolean withRoot) {
        String url = formatUrl("/file/list");
        Map<String, String> params = new HashMap<>(2);
        params.put("baseDir", baseDir);
        params.put("withRoot", String.valueOf(withRoot));
        JsonNode node = HttpUtils.post(url, params, wrapToken());
        return JsonUtils.toList(node, FileNode.class);
    }

    public String getFileContent(String path) {
        String url = formatUrl("/file/content");
        Map<String, String> params = new HashMap<>(2);
        params.put("path", path);
        JsonNode node = HttpUtils.post(url, params, wrapToken());
        return node.get("data").asText();
    }

    public void deleteFile(String path) {
        try {
            String encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.name());
            String url = formatUrl("/file/delete?path=" + encoded);
            HttpUtils.delete(url, wrapToken());
        } catch (UnsupportedEncodingException e) {
            throw new JarbootException(e);
        }
    }

    public String writeFileContent(String path, String content) {
        String url = formatUrl("/file/write");
        return wrapFileParam(path, content, url);
    }

    public String newFile(String path, String content) {
        String url = formatUrl("/file/create");
        return wrapFileParam(path, content, url);
    }

    public String newDirectory(String path) {
        String url = formatUrl("/directory");
        Map<String, String> params = new HashMap<>(2);
        params.put("path", path);
        JsonNode node = HttpUtils.post(url, params, wrapToken());
        return node.get("data").asText();
    }

    public void exportService(String name, OutputStream os) {
        try {
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
            String url = formatUrl("/exportService?name=" + encoded);
            HttpUtils.get(url, os, wrapToken());
        } catch (UnsupportedEncodingException e) {
            throw new JarbootException(e);
        }
    }

    public void importService(String name, InputStream is) {
        String url = formatUrl("/importService");
        HttpUtils.upload(url, is, name, null, wrapToken());
    }

    public void downloadAnyFile(String encode, OutputStream os) {
        String url = formatUrl("/download/" + encode);
        HttpUtils.get(url, os, wrapToken());
    }

    private String wrapFileParam(String path, String content, String url) {
        Map<String, String> params = new HashMap<>(2);
        params.put("path", path);
        params.put("content", content);
        JsonNode node = HttpUtils.post(url, params, wrapToken());
        return node.get("data").asText();
    }
    public ServerRuntimeInfo health() {
        String url;
        if (host.startsWith(CommonConst.HTTP) || host.startsWith(CommonConst.HTTPS)) {
            url = host + CommonConst.SERVER_RUNTIME_CONTEXT;
        } else {
            url = String.format("%s%s%s", CommonConst.HTTP, host, CommonConst.SERVER_RUNTIME_CONTEXT);
        }
        try {
            ServerRuntimeInfo info = HttpUtils.getObj(url, ServerRuntimeInfo.class, getInnerUserToken());
            state = ClusterServerState.ONLINE;
            return info;
        } catch (JarbootException e) {
            if (HttpServletResponse.SC_UNAUTHORIZED == e.getErrorCode()) {
                state = ClusterServerState.AUTH_FAILED;
                logger.warn("认证失败，{}与当前服务的cluster-secret-key不一致或正在启动中.", host);
            } else {
                state = ClusterServerState.OFFLINE;
            }
            throw new JarbootException(e);
        } catch (Exception e) {
            state = ClusterServerState.OFFLINE;
            throw new JarbootException(e);
        }
    }

    public String requestSync(ClusterEventName eventName, String body, long millis) {
        String id = UUID.randomUUID().toString();
        ClusterEventMessage req = new ClusterEventMessage();
        req.setId(id);
        req.setName(eventName.name());
        req.setType(ClusterEventMessage.REQ_TYPE);
        req.setBody(body);
        req.setNeedAck(true);

        try {
            RequestCallback requestCallback = new RequestCallback();
            requestCallback.countDownLatch = new CountDownLatch(1);
            requestCallback.id = id;
            requestCallback.reqBody = body;
            requestCallback.name = eventName.name();
            requestCallbackMap.putIfAbsent(id, requestCallback);
            sendMessage(req);
            if (requestCallback.countDownLatch.await(millis, TimeUnit.MILLISECONDS)) {
                return requestCallback.rspBody;
            } else {
                logger.error("请求{}超时,body:{}", eventName.name(), body);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            requestCallbackMap.remove(id);
        }
        return StringUtils.EMPTY;
    }

    public void handleMessage(ClusterEventMessage eventMessage) {
        TaskUtils.getTaskExecutor().execute(() -> {
            try {
                if (Objects.equals(ClusterEventMessage.REQ_TYPE, eventMessage.getType())) {
                    handleEvent(eventMessage);
                    return;
                }
                if (Objects.equals(ClusterEventMessage.RSP_TYPE, eventMessage.getType())) {
                    // 响应内容
                    onResponse(eventMessage);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    void sendMessage(ClusterEventMessage message) {
        String self = ClusterClientManager.getInstance().getSelfHost();
        if (StringUtils.isEmpty(self)) {
            throw new JarbootException("cluster is not enabled, self host is empty!");
        }
        String msgUrl = StringUtils.EMPTY;
        try {
            msgUrl = formatHandleMsgUrl();
            ResponseSimple resp = HttpUtils.postObj(msgUrl, message, ResponseSimple.class, getInnerUserToken());
            if (!resp.getSuccess()) {
                logger.error(resp.getMsg());
            }
        } catch (Exception e) {
            logger.error("name: {}, body: {}, msgUrl: {}, error:{}", message.getName(), message.getBody(), msgUrl, e.getMessage(), e);
        }
    }

    private static Map<String, String> getInnerUserToken() {
        String token = ClusterClientManager.getInstance().getClusterToken(AuthConst.JARBOOT_USER);
        Map<String, String> header = new HashMap<>(2);
        header.put(AuthConst.CLUSTER_TOKEN, token);
        return header;
    }

    private String formatHandleMsgUrl() throws UnsupportedEncodingException {
        String self = ClusterClientManager.getInstance().getSelfHost();
        String api = "/handleMessage/" + URLEncoder.encode(self, StandardCharsets.UTF_8.name());
        return formatUrl(api);
    }

    private void handleEvent(ClusterEventMessage eventMessage) {
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
            case CLUSTER_AUTH:
                resp = clusterAuth(eventMessage);
                break;
            default:
                logger.error("未找到处理方法：{}", eventMessage.getName());
                resp = "unknown message:" + eventMessage.getName();
                break;
        }
        if (eventMessage.isNeedAck()) {
            // 请求内容，并且需要回执
            ClusterEventMessage respEvent = new ClusterEventMessage();
            respEvent.setId(eventMessage.getId());
            respEvent.setName(eventMessage.getName());
            respEvent.setType(ClusterEventMessage.RSP_TYPE);
            respEvent.setBody(resp);
            sendMessage(respEvent);
        }
    }

    private static String clusterAuth(ClusterEventMessage eventMessage) {
        try (ByteArrayOutputStream bao = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bao)) {
            Authentication authentication = SettingUtils.getContext().getBean(JwtTokenManager.class).getAuthentication(eventMessage.getBody());
            oos.writeObject(authentication);
            bao.toByteArray();
            byte[] bytes = Base64.encodeBase64(bao.toByteArray());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private void onResponse(ClusterEventMessage eventMessage) {
        RequestCallback requestCallback = requestCallbackMap.get(eventMessage.getId());
        if (null != requestCallback) {
            requestCallback.rspBody = eventMessage.getBody();
            if (null != requestCallback.countDownLatch) {
                requestCallback.countDownLatch.countDown();
            }
        }
    }

    private String startLocalService(ClusterEventMessage eventMessage) {
        ServiceSetting setting = JsonUtils.readValue(eventMessage.getBody(), ServiceSetting.class);
        ServiceManagerImpl serviceManager = SettingUtils.getContext().getBean(ServiceManagerImpl.class);
        if (setting == null) {
            return JsonUtils.toJsonString(HttpResponseUtils.error(String.format("启动服务，解析服务配置失败，setting: %s", eventMessage.getBody())));
        } else {
            serviceManager.startSingleService(setting);
        }
        return JsonUtils.toJsonString(HttpResponseUtils.success());
    }

    private String stopLocalService(ClusterEventMessage eventMessage) {
        ServiceSetting setting = JsonUtils.readValue(eventMessage.getBody(), ServiceSetting.class);
        ServiceManagerImpl serviceManager = SettingUtils.getContext().getBean(ServiceManagerImpl.class);
        if (setting == null) {
            return JsonUtils.toJsonString(HttpResponseUtils.error(String.format("停止服务，解析服务配置失败，setting: %s", eventMessage.getBody())));
        } else {
            serviceManager.stopSingleService(setting);
        }
        return JsonUtils.toJsonString(HttpResponseUtils.success());
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
        NotifyReactor.getInstance().publishEvent(event);
    }

    private static void handleNotifyToFront(ClusterEventMessage eventMessage) {
        FromOtherClusterServerMessageEvent event = JsonUtils
                .readValue(eventMessage.getBody(), FromOtherClusterServerMessageEvent.class);
        NotifyReactor.getInstance().publishEvent(event);
    }

    private String formatUrl(String api) {
        String url;
        api = CommonConst.CLUSTER_API_CONTEXT + api;
        if (host.startsWith(CommonConst.HTTP) || host.startsWith(CommonConst.HTTPS)) {
            url = host + api;
        } else {
            url = String.format("%s%s%s", CommonConst.HTTP, host, api);
        }
        return url;
    }
    private Map<String, String> wrapToken() {
        String token = ClusterClientManager.getInstance().getClusterToken(SettingUtils.getCurrentLoginUsername());
        Map<String, String> header = new HashMap<>(2);
        header.put(AuthConst.CLUSTER_TOKEN, token);
        if (StringUtils.isNotEmpty(ClusterClientManager.getInstance().getSelfHost())) {
            String ip = ClusterClientManager.parseIp(ClusterClientManager.getInstance().getSelfHost());
            header.put("Proxy-Client-IP", ip);
        }
        return header;
    }

    private static class RequestCallback {
        CountDownLatch countDownLatch;
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
            throw new JarbootException(resp.getCode(), resp.getMsg());
        }
    }
}
