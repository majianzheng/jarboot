package com.mz.jarboot.core.utils;

import com.mz.jarboot.common.JsonUtils;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.constant.CoreConstant;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Http工具类
 * @author majianzheng
 */
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static final String BASE_URL =String.format("http://%s", EnvironmentContext.getHost());
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();

    public static void postSimple(String api, String json) {
        String url = BASE_URL + api;
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .post(requestBody);
        doRequest(requestBuilder);
    }

    public static void getSimple(String api) {
        String url = BASE_URL + api;
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .get();
        doRequest(requestBuilder);
    }

    private static void doRequest(Request.Builder requestBuilder) {
        requestBuilder.addHeader("Cookie", "");
        requestBuilder.addHeader("Accept", "application/json");
        requestBuilder.addHeader("Content-Type", "application/json;charset=UTF-8");
        Request request = requestBuilder.build();

        Call call = HTTP_CLIENT.newCall(request);
        try {
            ResponseBody response = call.execute().body();
            if (null != response) {
                String body = response.string();
                ResponseSimple resp = JsonUtils.readValue(body, ResponseSimple.class);
                if (null == resp) {
                    logger.error("返回结果解析json失败！{}", body);
                    return;
                }
                if (resp.getResultCode() != ResultCodeConst.SUCCESS) {
                    logger.error(resp.getResultMsg());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private HttpUtils() {}
}
