package com.mz.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.api.service.ServerMgrService;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.client.utlis.ResponseUtils;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务管理客户端
 * @author jianzhengma
 */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class ServerManager implements ServerMgrService {
    private final ClientProxy clientProxy;

    /**
     * 服务管理客户端构造
     * @param host 服务地址
     * @param user 用户名
     * @param password 登录密码
     */
    public ServerManager(String host, String user, String password) {
        if (null == user || null == password) {
            this.clientProxy = ClientProxy.Factory.createClientProxy(host);
        } else {
            this.clientProxy = ClientProxy.Factory.createClientProxy(host, user, password);
        }
    }

    @Override
    public List<ServerRunning> getServerList() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/getServerList";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode result = ResponseUtils.parseResult(response, api);
        List<ServerRunning> list = new ArrayList<>();
        final int size = result.size();
        for (int i = 0; i < size; ++i) {
            JsonNode node = result.get(i);
            ServerRunning serverRunning = JsonUtils.treeToValue(node, ServerRunning.class);
            list.add(serverRunning);
        }
        return list;
    }

    @Override
    public void oneClickRestart() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/oneClickRestart";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void oneClickStart() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/oneClickStart";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void oneClickStop() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/oneClickStop";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void startServer(List<String> paths) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/startServer";
        String json = JsonUtils.toJsonString(paths);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void stopServer(List<String> paths) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/stopServer";
        String json = JsonUtils.toJsonString(paths);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void restartServer(List<String> paths) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/restartServer";
        String json = JsonUtils.toJsonString(paths);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void startSingleServer(ServerSetting setting) {
        final String api = "/api/jarboot/plugin/debug/startServer";
        String json = JsonUtils.toJsonString(setting);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public List<JvmProcess> getJvmProcesses() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/getJvmProcesses";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
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
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/attach?pid=" + pid;
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    @Override
    public void deleteServer(String server) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/deleteServer?server=" + server;
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }
}
