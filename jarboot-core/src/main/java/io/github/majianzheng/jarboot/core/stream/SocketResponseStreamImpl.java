package io.github.majianzheng.jarboot.core.stream;

import io.github.majianzheng.jarboot.core.basic.WsClientFactory;

/**
 * 小数据量传输通过WebSocket
 * @author majianzheng
 */
public class SocketResponseStreamImpl implements ResponseStream {
    @Override
    public void write(byte[] data) {
        WsClientFactory.getInstance().send(data);
    }
}
