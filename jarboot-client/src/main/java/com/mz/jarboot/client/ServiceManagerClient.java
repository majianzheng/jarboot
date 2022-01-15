package com.mz.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.constant.TaskLifecycle;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.event.TaskLifecycleEvent;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServiceInstance;
import com.mz.jarboot.api.pojo.ServiceSetting;
import com.mz.jarboot.api.service.ServiceManager;
import com.mz.jarboot.client.utlis.HttpMethod;
import com.mz.jarboot.common.utils.ApiStringBuilder;
import com.mz.jarboot.client.utlis.ClientConst;
import com.mz.jarboot.client.utlis.ResponseUtils;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import okhttp3.FormBody;

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

    @Override
    public List<ServiceInstance> getServiceList() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/services";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode result = ResponseUtils.parseResult(response, api);
        List<ServiceInstance> list = new ArrayList<>();
        final int size = result.size();
        for (int i = 0; i < size; ++i) {
            JsonNode node = result.get(i);
            ServiceInstance serviceInstance = JsonUtils.treeToValue(node, ServiceInstance.class);
            list.add(serviceInstance);
        }
        return list;
    }

    @Override
    public void oneClickRestart() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/oneClickRestart";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void oneClickStart() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/oneClickStart";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void oneClickStop() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/oneClickStop";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void startService(List<String> serviceNames) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/startService";
        String json = JsonUtils.toJsonString(serviceNames);
        String response = this.clientProxy.reqApi(api, json, HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void stopService(List<String> serviceNames) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/stopService";
        String json = JsonUtils.toJsonString(serviceNames);
        String response = this.clientProxy.reqApi(api, json, HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void restartService(List<String> serviceNames) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/restartService";
        String json = JsonUtils.toJsonString(serviceNames);
        String response = this.clientProxy.reqApi(api, json, HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void startSingleService(ServiceSetting setting) {
        final String api = "/api/jarboot/plugin/debug/startBySetting";
        String json = JsonUtils.toJsonString(setting);
        String response = this.clientProxy.reqApi(api, json, HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public List<JvmProcess> getJvmProcesses() {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/jvmProcesses";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
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
        ApiStringBuilder asb = new ApiStringBuilder(CommonConst.SERVICE_MGR_CONTEXT, "/attach");
        asb.add(ClientConst.PID_PARAM, pid);
        final String api = asb.build();
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void deleteService(String serviceName) {
        final String api = CommonConst.SERVICE_MGR_CONTEXT + "/service";
        FormBody.Builder builder = new FormBody.Builder();
        builder.add(CommonConst.SERVICE_NAME_PARAM, serviceName);
        String response = this.clientProxy.reqApi(api, HttpMethod.DELETE, builder.build());
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    /**
     * 注册事件处理
     *
     * @param serviceName 服务名称
     * @param lifecycle   任务生命周期 {@link TaskLifecycle}
     * @param subscriber  任务处理 {@link Subscriber}
     */
    @Override
    public void registerSubscriber(String serviceName,
                                   TaskLifecycle lifecycle,
                                   Subscriber<TaskLifecycleEvent> subscriber) {
        final String topic = this.clientProxy.createLifecycleTopic(serviceName, lifecycle);
        this.clientProxy.registerSubscriber(topic, subscriber);
    }

    /**
     * 反注册事件处理
     *
     * @param serviceName 服务名称
     * @param lifecycle   任务生命周期 {@link TaskLifecycle}
     * @param subscriber  任务处理 {@link Subscriber}
     */
    @Override
    public void deregisterSubscriber(String serviceName,
                                     TaskLifecycle lifecycle,
                                     Subscriber<TaskLifecycleEvent> subscriber) {
        final String topic = this.clientProxy.createLifecycleTopic(serviceName, lifecycle);
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
        ApiStringBuilder asb = new ApiStringBuilder(CommonConst.SERVICE_MGR_CONTEXT, "/service");
        asb.add(CommonConst.SERVICE_NAME_PARAM, serviceName);
        final String api = asb.build();
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return JsonUtils.treeToValue(result, ServiceInstance.class);
    }
}
