package com.mz.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.GlobalSetting;
import com.mz.jarboot.api.pojo.ServerSetting;
import com.mz.jarboot.api.service.SettingService;
import com.mz.jarboot.client.utlis.HttpMethod;
import com.mz.jarboot.common.utils.ApiStringBuilder;
import com.mz.jarboot.client.utlis.ClientConst;
import com.mz.jarboot.client.utlis.ResponseUtils;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.utils.StringUtils;
import okhttp3.FormBody;

/**
 * @author majianzheng
 */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class SettingClient implements SettingService {
    private final ClientProxy clientProxy;

    /**
     * 服务管理客户端构造
     * @param host 服务地址
     * @param user 用户名
     * @param password 登录密码
     */
    public SettingClient(String host, String user, String password) {
        if (null == user || null == password) {
            this.clientProxy = ClientProxy.Factory.createClientProxy(host);
        } else {
            this.clientProxy = ClientProxy.Factory.createClientProxy(host, user, password);
        }
    }

    /**
     * 获取服务配置
     *
     * @param path 服务路径
     * @return 配置信息
     */
    @Override
    public ServerSetting getServerSetting(String path) {
        ApiStringBuilder asb = new ApiStringBuilder(CommonConst.SETTING_CONTEXT, "/serverSetting");
        final String api = asb.add(ClientConst.PATH_PARAM, path).build();
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return JsonUtils.treeToValue(result, ServerSetting.class);
    }

    /**
     * 提交服务配置
     *
     * @param setting 配置
     */
    @Override
    public void submitServerSetting(ServerSetting setting) {
        final String api = CommonConst.SETTING_CONTEXT + "/serverSetting";
        String body = JsonUtils.toJsonString(setting);
        String response = this.clientProxy.reqApi(api, body, HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    /**
     * 获取全局配置
     *
     * @return 配置
     */
    @Override
    public GlobalSetting getGlobalSetting() {
        final String api = CommonConst.SETTING_CONTEXT + "/globalSetting";
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return JsonUtils.treeToValue(result, GlobalSetting.class);
    }

    /**
     * 提交全局配置
     *
     * @param setting 配置
     */
    @Override
    public void submitGlobalSetting(GlobalSetting setting) {
        final String api = CommonConst.SETTING_CONTEXT + "/globalSetting";
        String body = JsonUtils.toJsonString(setting);
        String response = this.clientProxy.reqApi(api, body, HttpMethod.POST);
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    /**
     * 获取vm options
     *
     * @param path 服务路径
     * @param file 文件
     * @return vm
     */
    @Override
    public String getVmOptions(String path, String file) {
        ApiStringBuilder asb = new ApiStringBuilder(CommonConst.SETTING_CONTEXT, "/vmoptions");
        final String api = asb.add(ClientConst.PATH_PARAM, path).add(ClientConst.FILE_PARAM, file).build();
        String response = this.clientProxy.reqApi(api, StringUtils.EMPTY, HttpMethod.GET);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return result.asText(StringUtils.EMPTY);
    }

    /**
     * 保存vm options
     *
     * @param path  服务
     * @param file    文件
     * @param content 文件内容
     */
    @Override
    public void saveVmOptions(String path, String file, String content) {
        final String api = CommonConst.SETTING_CONTEXT + "/vmoptions";
        FormBody.Builder builder = new FormBody.Builder();
        builder
                .add(ClientConst.PATH_PARAM, path)
                .add(ClientConst.FILE_PARAM, file)
                .add(ClientConst.CONTENT_PARAM, content);
        String response = this.clientProxy.reqApi(api, HttpMethod.POST, builder.build());
        JsonNode jsonNode = JsonUtils.readAsJsonNode(response);
        ResponseUtils.checkResponse(api, jsonNode);
    }
}
