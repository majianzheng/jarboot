package com.mz.jarboot.core.utils;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.utils.BasicTypeConvert;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.pojo.ResponseSimple;
import com.mz.jarboot.common.pojo.ResultCodeConst;
import com.mz.jarboot.common.utils.StringUtils;
import com.mz.jarboot.core.basic.EnvironmentContext;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

/**
 * Http工具类
 * @author majianzheng
 */
public class HttpUtils {
    /** 服务基址 */
    private static String baseUrl = (CommonConst.HTTP +
            System.getProperty(CommonConst.REMOTE_PROP, "127.0.0.1:9899"));
    /** 连接超时 */
    private static final long CONNECT_TIMEOUT = 10L;
    /** 写超时 */
    private static final long READ_WRITE_TIMEOUT = 5L;
    /** application json media */
    private static final String JSON_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(JSON_TYPE);
    /** 连接池实例 */
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(false)
            .dispatcher(new Dispatcher(EnvironmentContext.getScheduledExecutor()))
            .build();

    /**
     * Post请求
     * @param url api接口
     * @param object 传入的参数
     * @param type 期望的结果类型
     * @param <T> 范型类
     * @return 期望的结构
     */
    public static <T> T postObj(String url, Object object, Class<T> type) {
        String json = object instanceof String ? (String)object : JsonUtils.toJsonString(object);
        if (null == json) {
            json = "";
        }
        RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, json);
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .post(requestBody);
        String resp = doRequest(requestBuilder);
        if (type.isPrimitive()) {
            return BasicTypeConvert.convert(resp, type);
        }
        return JsonUtils.readValue(resp, type);
    }

    /**
     * Post请求Simple
     * @param api api接口
     * @param buf 传入的参数
     */
    public static void postSimple(String api, byte[] buf) {
        String url = baseUrl + api;
        RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, buf);
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .post(requestBody);
        String resp = doRequest(requestBuilder);
        checkSimple(JsonUtils.readValue(resp, ResponseSimple.class));
    }

    /**
     * Get请求
     * @param url api接口
     * @param type 期望的结果类型
     * @param <T> 范型类
     * @return 期望的结构
     */
    public static <T> T getObj(String url, Class<T> type) {
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .get();
        String resp = doRequest(requestBuilder);
        if (type.isPrimitive()) {
            return BasicTypeConvert.convert(resp, type);
        }
        return JsonUtils.readValue(resp, type);
    }

    /**
     * Get请求
     * @param url url
     * @return response
     */
    public static String getString(String url) {
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .get();
        return doRequest(requestBuilder);
    }

    /**
     * Get请求Simple
     * @param api api接口
     */
    public static void getSimple(String api) {
        String url = baseUrl + api;
        ResponseSimple resp = getObj(url, ResponseSimple.class);
        checkSimple(resp);
    }

    /**
     * 设定服务基址
     * @param baseUrl Jarboot服务地址：http://127.0.0.1:9899
     */
    public static void setBaseUrl(String baseUrl) {
        HttpUtils.baseUrl = baseUrl;
    }

    private static void checkSimple(ResponseSimple resp) {
        if (null == resp) {
            throw new JarbootException("返回结果解析json失败!");
        }
        if (resp.getCode() != ResultCodeConst.SUCCESS) {
            throw new JarbootException(resp.getCode(), resp.getMsg());
        }
    }

    private static String doRequest(Request.Builder requestBuilder) {
        requestBuilder.addHeader("Cookie", "");
        requestBuilder.addHeader("Accept", JSON_TYPE);
        requestBuilder.addHeader("Content-Type", "application/json;charset=UTF-8");
        Request request = requestBuilder.build();

        Call call = HTTP_CLIENT.newCall(request);
        try {
            ResponseBody response = call.execute().body();
            if (null != response) {
                return response.string();
            }
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private HttpUtils() {}
}
