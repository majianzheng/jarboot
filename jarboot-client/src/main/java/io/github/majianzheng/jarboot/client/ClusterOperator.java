package io.github.majianzheng.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.api.pojo.HostInfo;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.client.utlis.ResponseUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;

import java.util.List;

/**
 * 集群操作
 * @author majianzheng
 */
public class ClusterOperator {
    private final ClientProxy clientProxy;

    /**
     * 服务管理客户端构造
     * @param host 服务地址
     * @param user 用户名
     * @param password 登录密码
     */
    public ClusterOperator(String host, String user, String password) {
        if (null == user || null == password) {
            this.clientProxy = ClientProxy.Factory.createClientProxy(host);
        } else {
            this.clientProxy = ClientProxy.Factory.createClientProxy(host, user, password);
        }
    }

    /**
     * 服务管理客户端构造
     * @param proxy 客户端代理类
     */
    public ClusterOperator(ClientProxy proxy) {
        this.clientProxy = proxy;
    }

    public List<HostInfo> getOnlineClusterHosts() {
        JsonNode response = this.clientProxy.get(CommonConst.CLUSTER_MGR_CONTEXT + "/onlineClusterHosts");
        return ResponseUtils.getListResult(response, HostInfo.class);
    }
    public List<ServiceInstance> getServiceGroup() {
        JsonNode response = this.clientProxy.get(CommonConst.CLUSTER_MGR_CONTEXT + "/services");
        return ResponseUtils.getListResult(response, ServiceInstance.class);
    }

    public void startService(List<ServiceInstance> service) {
        final String api = CommonConst.CLUSTER_MGR_CONTEXT + "/startServices";
        JsonNode response = this.clientProxy.postJson(api, service);
        ResponseUtils.checkResponse(api, response);
    }

    public void stopService(List<ServiceInstance> service) {
        final String api = CommonConst.CLUSTER_MGR_CONTEXT + "/stopServices";
        JsonNode response = this.clientProxy.postJson(api, service);
        ResponseUtils.checkResponse(api, response);
    }

    public void restartService(List<ServiceInstance> service) {
        final String api = CommonConst.CLUSTER_MGR_CONTEXT + "/restartServices";
        JsonNode response = this.clientProxy.postJson(api, service);
        ResponseUtils.checkResponse(api, response);
    }

    public String getUserDir() {
        final String api = CommonConst.AUTH_CONTEXT + "/getCurrentUser";
        JsonNode response = this.clientProxy.get(api);
        JsonNode data = ResponseUtils.parseResult(response, api);
        return data.get("userDir").asText("");
    }

    public void deleteService(List<ServiceInstance> service) {
        final String api = CommonConst.CLUSTER_MGR_CONTEXT + "/deleteService";
        JsonNode response = this.clientProxy.postJson(api, service);
        ResponseUtils.checkResponse(api, response);
    }

    public ServiceSetting getServiceSetting(ServiceInstance service) {
        final String api = CommonConst.CLUSTER_MGR_CONTEXT + "/serviceSetting";
        JsonNode response = this.clientProxy.postJson(api, service);
        JsonNode result = ResponseUtils.parseResult(response, CommonConst.SERVICE_MGR_CONTEXT);
        return JsonUtils.treeToValue(result, ServiceSetting.class);
    }

    /**
     * 注册事件处理
     *
     * @param subscriber  任务处理 {@link Subscriber}
     */
    public void registerSubscriber(Subscriber<TaskLifecycleEvent> subscriber) {
        final String topic = this.clientProxy.createTopic(TaskLifecycleEvent.class);
        this.clientProxy.registerSubscriber(topic, subscriber);
    }

    /**
     * 反注册事件处理
     *
     * @param subscriber  任务处理 {@link Subscriber}
     */
    public void deregisterSubscriber(Subscriber<TaskLifecycleEvent> subscriber) {
        final String topic = this.clientProxy.createTopic(TaskLifecycleEvent.class);
        this.clientProxy.deregisterSubscriber(topic, subscriber);
    }
}
