package io.github.majianzheng.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.TaskLifecycle;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.api.pojo.JvmProcess;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.common.utils.ApiStringBuilder;
import io.github.majianzheng.jarboot.client.utlis.ClientConst;
import io.github.majianzheng.jarboot.client.utlis.ResponseUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务管理客户端
 * @author jianzhengma
 */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class ServiceManagerClient implements ServiceManager {
    private final ClientProxy clientProxy;

    /**
     * 服务管理客户端构造
     * @param host 服务地址
     * @param user 用户名
     * @param password 登录密码
     */
    public ServiceManagerClient(String host, String user, String password) {
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
    public ServiceManagerClient(ClientProxy proxy) {
        this.clientProxy = proxy;
    }

    @Override
    public List<ServiceInstance> getServiceList() {
        JsonNode response = this.clientProxy.get(CommonConst.SERVICE_MGR_CONTEXT);
        return ResponseUtils.getListResult(response, ServiceInstance.class);
    }

    @Override
    public ServiceInstance getServiceGroup() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/groups";
        return doGetGroups(api, ServiceInstance.class);
    }

    @Override
    public JvmProcess getJvmGroup() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/jvmGroups";
        return doGetGroups(api, JvmProcess.class);
    }

    private <T> T doGetGroups(String api, Class<T> cls) {
        JsonNode response = this.clientProxy.get(api);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return JsonUtils.treeToValue(result, cls);
    }

    @Override
    public void startService(List<String> serviceNames) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/startService";
        JsonNode response = this.clientProxy.postJson(api, serviceNames);
        ResponseUtils.checkResponse(api, response);
    }

    @Override
    public void stopService(List<String> serviceNames) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/stopService";
        JsonNode response = this.clientProxy.postJson(api, serviceNames);
        ResponseUtils.checkResponse(api, response);
    }

    @Override
    public void restartService(List<String> serviceNames) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/restartService";
        JsonNode response = this.clientProxy.postJson(api, serviceNames);
        ResponseUtils.checkResponse(api, response);
    }

    @Override
    public void startSingleService(ServiceSetting setting) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/startSingleService";
        JsonNode response = this.clientProxy.postJson(api, setting);
        ResponseUtils.checkResponse(api, response);
    }

    @Override
    public void stopSingleService(ServiceSetting setting) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/stopSingleService";
        JsonNode response = this.clientProxy.postJson(api, setting);
        ResponseUtils.checkResponse(api, response);
    }

    @Override
    public List<JvmProcess> getJvmProcesses() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/jvmProcesses";
        JsonNode response = this.clientProxy.get(api);
        JsonNode result = ResponseUtils.parseResult(response, api);
        List<JvmProcess> list = new ArrayList<>();
        final int size = result.size();
        for (int i = 0; i < size; ++i) {
            JsonNode node = result.get(i);
            JvmProcess serverRunning = JsonUtils.treeToValue(node, JvmProcess.class);
            list.add(serverRunning);
        }
        return list;
    }

    @Override
    public void attach(String pid) {
        final String api = new ApiStringBuilder(CommonConst.SERVICE_MGR_CONTEXT, "/attach")
                .add(ClientConst.PID_PARAM, pid)
                .build();
        JsonNode response = this.clientProxy.get(api);
        ResponseUtils.checkResponse(api, response);
    }

    @Override
    public void deleteService(String serviceName) {
        final String api = new ApiStringBuilder(CommonConst.SERVICE_MGR_CONTEXT + "/service")
                .add(CommonConst.SERVICE_NAME_PARAM, serviceName)
                .build();
        JsonNode response = this.clientProxy.delete(api);
        ResponseUtils.checkResponse(api, response);
    }

    /**
     * 注册事件处理
     *
     * @param host        服务地址
     * @param serviceName 服务名称
     * @param lifecycle   任务生命周期 {@link TaskLifecycle}
     * @param subscriber  任务处理 {@link Subscriber}
     */
    @Override
    public void registerSubscriber(String host,
                                   String serviceName,
                                   TaskLifecycle lifecycle,
                                   Subscriber<TaskLifecycleEvent> subscriber) {
        if (StringUtils.isNotEmpty(host)) {
            serviceName = serviceName + "@" + host;
        }
        final String topic = this.clientProxy.createTopic(TaskLifecycleEvent.class, serviceName, lifecycle.name());
        this.clientProxy.registerSubscriber(topic, subscriber);
    }

    /**
     * 反注册事件处理
     *
     * @param host        服务地址
     * @param serviceName 服务名称
     * @param lifecycle   任务生命周期 {@link TaskLifecycle}
     * @param subscriber  任务处理 {@link Subscriber}
     */
    @Override
    public void deregisterSubscriber(String host,
                                     String serviceName,
                                     TaskLifecycle lifecycle,
                                     Subscriber<TaskLifecycleEvent> subscriber) {
        if (StringUtils.isNotEmpty(host)) {
            serviceName = serviceName + "@" + host;
        }
        final String topic = this.clientProxy.createTopic(TaskLifecycleEvent.class, serviceName, lifecycle.name());
        this.clientProxy.deregisterSubscriber(topic, subscriber);
    }

    /**
     * 获取服务信息
     *
     * @param serviceName 服务名称
     * @return 服务信息 {@link ServiceInstance}
     */
    @Override
    public ServiceInstance getService(String serviceName) {
        final String api = new ApiStringBuilder(CommonConst.SERVICE_MGR_CONTEXT, "/service")
                .add(CommonConst.SERVICE_NAME_PARAM, serviceName)
                .build();
        JsonNode response = this.clientProxy.get(api);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return JsonUtils.treeToValue(result, ServiceInstance.class);
    }
}
