package com.mz.jarboot.core.stream;

import com.mz.jarboot.core.constant.CoreConstant;

/**
 * ResponseStream delegate
 * @author majianzheng
 */
public class ResponseStreamDelegate implements ResponseStream {
    private final ResponseStream http = new HttpResponseStreamImpl();
    private final ResponseStream socket = new SocketResponseStreamImpl();
    /**
     * 写响应数据
     *
     * @param data 数据
     */
    @Override
    public void write(byte[] data) {
        ResponseStream stream = (data.length < CoreConstant.SOCKET_MAX_SEND) ? socket : http;
        stream.write(data);
    }
}
