package io.github.majianzheng.jarboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.SystemSetting;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.common.utils.ApiStringBuilder;
import io.github.majianzheng.jarboot.client.utlis.ClientConst;
import io.github.majianzheng.jarboot.client.utlis.ResponseUtils;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author majianzheng
 */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class SettingClient implements SettingService {
    private final ClientProxy clientProxy;

    /**
     * 服务配置客户端构造
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
     * 服务配置客户端构造
     * @param proxy 客户端代理类
     */
    public SettingClient(ClientProxy proxy) {
        this.clientProxy = proxy;
    }

    /**
     * 获取服务配置
     *
     * @param serviceName 服务路径
     * @return 配置信息
     */
    @Override
    public ServiceSetting getServiceSetting(String serviceName) {
        ApiStringBuilder asb = new ApiStringBuilder(CommonConst.SETTING_CONTEXT, "/serviceSetting");
        final String api = asb.add(CommonConst.SERVICE_NAME_PARAM, serviceName).build();
        JsonNode response = this.clientProxy.get(api);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return JsonUtils.treeToValue(result, ServiceSetting.class);
    }

    /**
     * 提交服务配置
     *
     * @param setting 配置
     */
    @Override
    public void submitServiceSetting(ServiceSetting setting) {
        final String api = CommonConst.SETTING_CONTEXT + "/serviceSetting";
        JsonNode jsonNode = this.clientProxy.postJson(api, setting);
        ResponseUtils.checkResponse(api, jsonNode);
    }

    /**
     * 获取全局配置
     *
     * @return 配置
     */
    @Override
    public SystemSetting getSystemSetting() {
        final String api = CommonConst.SETTING_CONTEXT + "/globalSetting";
        JsonNode response = this.clientProxy.get(api);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return JsonUtils.treeToValue(result, SystemSetting.class);
    }

    /**
     * 提交全局配置
     *
     * @param setting 配置
     */
    @Override
    public void saveSetting(SystemSetting setting) {
        final String api = CommonConst.SETTING_CONTEXT + "/globalSetting";
        JsonNode response = this.clientProxy.postJson(api, setting);
        ResponseUtils.checkResponse(api, response);
    }

    /**
     * 获取vm options
     *
     * @param serviceName 服务路径
     * @param file 文件
     * @return vm
     */
    @Override
    public String getVmOptions(String serviceName, String file) {
        final String api = new ApiStringBuilder(CommonConst.SETTING_CONTEXT, "/vmoptions")
                .add(CommonConst.SERVICE_NAME_PARAM, serviceName)
                .add(ClientConst.FILE_PARAM, file)
                .build();
        JsonNode response = this.clientProxy.get(api);
        JsonNode result = ResponseUtils.parseResult(response, api);
        return result.asText(StringUtils.EMPTY);
    }

    /**
     * 保存vm options
     *
     * @param serviceName  服务
     * @param file    文件
     * @param content 文件内容
     */
    @Override
    public void saveVmOptions(String serviceName, String file, String content) {
        final String api = CommonConst.SETTING_CONTEXT + "/vmoptions";
        Map<String, String> formData = new HashMap<>(8);
        formData.put(CommonConst.SERVICE_NAME_PARAM, serviceName);
        formData.put(ClientConst.FILE_PARAM, file);
        formData.put(ClientConst.CONTENT_PARAM, content);

        JsonNode response = this.clientProxy.postForm(api, formData);
        ResponseUtils.checkResponse(api, response);
    }
}
