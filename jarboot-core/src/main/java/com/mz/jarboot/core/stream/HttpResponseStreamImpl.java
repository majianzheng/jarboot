package com.mz.jarboot.core.stream;

import com.alibaba.fastjson.JSON;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResultCodeConst;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.constant.CoreConstant;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 大数据量传输通过http协议，使用WebSocket会增加额外的拆包、组包实现增加业务复杂性
 * 快慢个几毫秒眼睛也分辨不出来
 * @author jianzhengma
 */
public class HttpResponseStreamImpl implements ResponseStream {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static final String API = "api/agent/response?server";
    private static final String RESP_URL =String.format("http://%s/%s=%s",
            EnvironmentContext.getHost(), API, EnvironmentContext.getServer());
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();
    @Override
    public void write(String data) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), data);
        Request.Builder requestBuilder = new Request
                .Builder()
                .url(RESP_URL)
                .post(requestBody);
        requestBuilder.addHeader("Cookie", "");
        requestBuilder.addHeader("Accept", "application/json");
        requestBuilder.addHeader("Content-Type", "application/json;charset=UTF-8");
        Request request = requestBuilder.build();

        Call call = httpClient.newCall(request);
        try {
            ResponseBody response = call.execute().body();
            if (null != response) {
                String body = response.string();
                ResponseSimple resp = JSON.parseObject(body, ResponseSimple.class);
                if (resp.getResultCode() != ResultCodeConst.SUCCESS) {
                    logger.error(resp.getResultMsg());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
