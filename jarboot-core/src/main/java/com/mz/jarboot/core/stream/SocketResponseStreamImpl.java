package com.mz.jarboot.core.stream;

import com.mz.jarboot.core.basic.WsClientFactory;
import okio.ByteString;

/**
 * 小数据量传输通过WebSocket
 * @author majianzheng
 */
public class SocketResponseStreamImpl implements ResponseStream {
    @Override
    public void write(byte[] data) {
        WsClientFactory
                .getInstance()
                .getSingletonClient()
                .send(ByteString.of(data, 0, data.length));
    }
}
