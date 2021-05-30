package com.mz.jarboot.core.resp;

import com.mz.jarboot.core.basic.SingletonCoreFactory;
import com.mz.jarboot.core.ws.WebSocketClient;

public class SocketResponseStreamImpl implements ResponseStream {
    private WebSocketClient client;
    public SocketResponseStreamImpl() {
        client = SingletonCoreFactory.getInstance().getSingletonClient();
    }
    @Override
    public void write(String data) {
        client.sendText(data);
    }
}
