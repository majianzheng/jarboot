package com.mz.jarboot.core.utils;

import com.mz.jarboot.common.JsonUtils;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.core.basic.EnvironmentContext;
import okhttp3.*;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Http工具类
 * @author majianzheng
 */
public class HttpUtils {
    private static final Logger logger = LogUtils.getLogger();
    private static final String BASE_URL =String.format("http://%s", EnvironmentContext.getHost());
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();

    public static <T> T postJson(String api, Object object, Class<T> type) {
        String url = BASE_URL + api;
        String json = object instanceof String ? (String)object : JsonUtils.toJsonString(object);
        if (null == json) {
            json = "";
        }
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .post(requestBody);
        return doRequest(requestBuilder, type);
    }

    public static void postSimple(String api, String json) {
        ResponseSimple resp = postJson(api, json, ResponseSimple.class);
        checkSimple(resp);
    }

    public static void getSimple(String api) {
        String url = BASE_URL + api;
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .get();
        ResponseSimple resp = doRequest(requestBuilder, ResponseSimple.class);
        checkSimple(resp);
    }

    private static void checkSimple(ResponseSimple resp) {
        if (null == resp) {
            logger.error("返回结果解析json失败!");
            return;
        }
        if (resp.getResultCode() != ResultCodeConst.SUCCESS) {
            logger.error(resp.getResultMsg());
        }
    }

    private static <T> T doRequest(Request.Builder requestBuilder, Class<T> type) {
        requestBuilder.addHeader("Cookie", "");
        requestBuilder.addHeader("Accept", "application/json");
        requestBuilder.addHeader("Content-Type", "application/json;charset=UTF-8");
        Request request = requestBuilder.build();

        Call call = HTTP_CLIENT.newCall(request);
        T resp = null;
        try {
            ResponseBody response = call.execute().body();
            if (null != response) {
                String body = response.string();
                resp = JsonUtils.readValue(body, type);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return resp;
    }

    private HttpUtils() {}
}
