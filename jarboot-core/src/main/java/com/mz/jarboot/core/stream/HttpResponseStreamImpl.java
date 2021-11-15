package com.mz.jarboot.core.stream;

import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.utils.HttpUtils;

/**
 * 大数据量传输通过http协议，使用WebSocket会增加额外的拆包、组包实现增加业务复杂性
 * 快慢个几毫秒眼睛也分辨不出来
 * @author majianzheng
 */
public class HttpResponseStreamImpl implements ResponseStream {

    private static class HttpResponseStreamImplHolder {
        static String api = "/api/public/agent/response?server=" + EnvironmentContext.getServer() +
                "&sid=" + EnvironmentContext.getSid();
    }

    @Override
    public void write(String data) {
        HttpUtils.postSimple(HttpResponseStreamImplHolder.api, data);
    }
}
