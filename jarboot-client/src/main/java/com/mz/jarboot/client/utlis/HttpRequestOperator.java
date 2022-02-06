package com.mz.jarboot.client.utlis;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.utils.StringUtils;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

/**
 * @author jianzhengma
 */
public class HttpRequestOperator {
    /** 连接超时 */
    public static final long CONNECT_TIMEOUT = 15L;
    /** 写超时 */
    private static final long READ_WRITE_TIMEOUT = 15L;
    /** 连接池实例 */
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();

    /**
     * post string json
     * @param url 请求地址
     * @param json json字符串
     * @param headers 请求头
     * @param method 请求方法
     * @return 请求结果
     */
    public static String req(String url, String json, okhttp3.Headers headers, HttpMethod method) {
        RequestBody requestBody = createJson(json);
        return req(url, method, requestBody, headers);
    }

    /**
     * 请求 api
     * @param url 请求地址
     * @param method 方法 {@link HttpMethod}
     * @param requestBody 请求消息体
     * @param headers 请求头
     * @return 请求结果
     */
    public static String req(String url,
                             HttpMethod method,
                             RequestBody requestBody,
                             okhttp3.Headers headers) {
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url);
        if (null != headers) {
            requestBuilder.headers(headers);
        }
        switch (method) {
            case GET:
                requestBuilder.get();
                break;
            case PUT:
                requestBuilder.put(requestBody);
                break;
            case POST:
                requestBuilder.post(requestBody);
                break;
            case DELETE:
                requestBuilder.delete(requestBody);
                break;
            default:
                break;
        }
        Request request = requestBuilder.build();

        return callUrl(request);
    }

    private static RequestBody createJson(String json) {
        RequestBody requestBody = null;
        if (StringUtils.isNotEmpty(json)) {
            requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        }
        return requestBody;
    }

    private static String callUrl(Request request) {
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

    private HttpRequestOperator() {}
}
