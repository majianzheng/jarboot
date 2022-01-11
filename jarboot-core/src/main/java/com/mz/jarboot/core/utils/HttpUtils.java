package com.mz.jarboot.core.utils;

import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResultCodeConst;
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
            .build();

    /**
     * Post请求
     * @param url api接口
     * @param object 传入的参数
     * @param type 期望的结果类型
     * @param <T> 范型类
     * @return 期望的结构
     */
    public static <T> T postJson(String url, Object object, Class<T> type) {
        String json = object instanceof String ? (String)object : JsonUtils.toJsonString(object);
        if (null == json) {
            json = "";
        }
        RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, json);
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .post(requestBody);
        return doRequest(requestBuilder, type);
    }

    /**
     * Post请求Simple
     * @param api api接口
     * @param json 传入的参数
     */
    public static void postSimple(String api, String json) {
        String url = baseUrl + api;
        ResponseSimple resp = postJson(url, json, ResponseSimple.class);
        checkSimple(resp);
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
        checkSimple(doRequest(requestBuilder, ResponseSimple.class));
    }

    /**
     * Get请求
     * @param url api接口
     * @param type 期望的结果类型
     * @param <T> 范型类
     * @return 期望的结构
     */
    public static <T> T getJson(String url, Class<T> type) {
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .get();
        return doRequest(requestBuilder, type);
    }

    /**
     * Get请求Simple
     * @param api api接口
     */
    public static void getSimple(String api) {
        String url = baseUrl + api;
        ResponseSimple resp = getJson(url, ResponseSimple.class);
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
        if (resp.getResultCode() != ResultCodeConst.SUCCESS) {
            throw new JarbootException(resp.getResultCode(), resp.getResultMsg());
        }
    }

    private static <T> T doRequest(Request.Builder requestBuilder, Class<T> type) {
        requestBuilder.addHeader("Cookie", "");
        requestBuilder.addHeader("Accept", JSON_TYPE);
        requestBuilder.addHeader("Content-Type", "application/json;charset=UTF-8");
        Request request = requestBuilder.build();

        Call call = HTTP_CLIENT.newCall(request);
        T resp = null;
        try {
            ResponseBody response = call.execute().body();
            if (null != response) {
                String body = response.string();
                if (type.isPrimitive()) {
                    return BasicTypeConvert.convert(body, type);
                }
                if (String.class.equals(type)) {
                    return (T) body;
                }
                resp = JsonUtils.readValue(body, type);
            }
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
        return resp;
    }

    private HttpUtils() {}
}
