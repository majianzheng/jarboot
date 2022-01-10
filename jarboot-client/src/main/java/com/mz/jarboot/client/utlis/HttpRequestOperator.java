package com.mz.jarboot.client.utlis;

import com.mz.jarboot.common.JarbootException;
import com.mz.jarboot.common.utils.StringUtils;
import okhttp3.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author jianzhengma
 */
public class HttpRequestOperator {
    /** 连接超时 */
    private static final long CONNECT_TIMEOUT = 15L;
    /** 写超时 */
    private static final long READ_WRITE_TIMEOUT = 15L;
    /** 连接池实例 */
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();

    public enum HttpMethod {
        /** Http GET */
        GET,
        /** Http POST */
        POST,
        /** Http PUT */
        PUT,
        /** Http DELETE */
        DELETE
    }

    /**
     * post form
     * @param url 请求地址
     * @param json json字符串
     * @param headers 请求头
     * @param method 请求方法
     * @return 请求结果
     */
    public static String reqJson(String url, String json, Map<String, String> headers, HttpMethod method) {
        RequestBody requestBody = createJson(json);
        return doRequest(url, method, requestBody, headers);
    }

    /**
     * post form
     * @param url 请求地址
     * @param form form表单
     * @param headers 请求头
     * @param method 请求方法
     * @return 请求结果
     */
    public static String req(String url, Map<String, String> form, Map<String, String> headers, HttpMethod method) {
        RequestBody requestBody = createForm(form);
        return doRequest(url, method, requestBody, headers);
    }

    public static String doRequest(String url,
                                   HttpMethod method,
                                   RequestBody requestBody,
                                   Map<String, String> headers) {
        okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
        if (null == headers) {
            headersBuilder.add("Cookie", "");
            headersBuilder.add("Accept", "*/*");
            headersBuilder.add("Content-Type", "application/json;charset=UTF-8");
        } else {
            headers.forEach(headersBuilder::add);
        }

        Request.Builder requestBuilder = new Request
                .Builder()
                .url(url)
                .headers(headersBuilder.build());
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

    private static RequestBody createForm(Map<String, String> form) {
        RequestBody requestBody = null;
        if (null != form) {
            FormBody.Builder builder = new FormBody.Builder();
            form.forEach(builder::add);
            requestBody = builder.build();
        }
        return requestBody;
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
}
