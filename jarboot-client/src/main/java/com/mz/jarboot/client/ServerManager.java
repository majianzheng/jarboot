package com.mz.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.api.service.ServerMgrService;
import com.mz.jarboot.client.utlis.ClientConst;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.common.ResultCodeConst;
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
        this.clientProxy = ClientProxy.Factory.createClientProxy(host, user, password);
    }

    @Override
    public List<ServerRunning> getServerList() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/getServerList";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode result = parseResult(response, api);
        List<ServerRunning> list = new ArrayList<>();
        final int size = result.size();
        for (int i = 0; i < size; ++i) {
            JsonNode node = result.get(i);
            ServerRunning serverRunning = JsonUtils.readValue(node.toString(), ServerRunning.class);
            list.add(serverRunning);
        }
        return list;
    }

    @Override
    public void oneClickRestart() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/oneClickRestart";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public void oneClickStart() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/oneClickStart";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public void oneClickStop() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/oneClickStop";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public void startServer(List<String> paths) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/startServer";
        String json = JsonUtils.toJsonString(paths);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public void stopServer(List<String> paths) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/stopServer";
        String json = JsonUtils.toJsonString(paths);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public void restartServer(List<String> paths) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/restartServer";
        String json = JsonUtils.toJsonString(paths);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public void startSingleServer(ServerSetting setting) {
        final String api = "/api/jarboot/plugin/debug/startServer";
        String json = JsonUtils.toJsonString(setting);
        String response = this.clientProxy.reqApi(api, json, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public List<JvmProcess> getJvmProcesses() {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/getJvmProcesses";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        JsonNode result = parseResult(response, api);
        List<JvmProcess> list = new ArrayList<>();
        final int size = result.size();
        for (int i = 0; i < size; ++i) {
            JsonNode node = result.get(i);
            JvmProcess serverRunning = JsonUtils.readValue(node.toString(), JvmProcess.class);
            list.add(serverRunning);
        }
        return list;
    }

    @Override
    public void attach(String pid) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/attach?pid=" + pid;
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    @Override
    public void deleteServer(String server) {
        final String api = CommonConst.SERVER_MGR_CONTEXT + "/deleteServer?server=" + server;
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
    }

    private JsonNode parseResult(String response, String api) {
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        checkResponse(api, jsonNode);
        JsonNode result = jsonNode.get(ClientConst.RESULT_KEY);
        if (null == result) {
            String msg = String.format("Request %s empty. response:%s", api, response);
            throw new JarbootRunException(msg);
        }
        return result;
    }

    private void checkResponse(String api, JsonNode jsonNode) {
        if (null == jsonNode) {
            throw new JarbootRunException("Request failed!" + api);
        }
        final int resultCode = jsonNode.get(ClientConst.RESULT_CODE_KEY).asInt(ResultCodeConst.INTERNAL_ERROR);
        if (ResultCodeConst.SUCCESS != resultCode) {
            String msg = String.format("Request %s failed. resultMsg:%s",
                    api, jsonNode.get("").asText(StringUtils.EMPTY));
            throw new JarbootRunException(msg);
        }
    }
}
