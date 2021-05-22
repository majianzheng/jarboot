package com.mz.jarboot.core.ws;

import io.netty.channel.Channel;

/**
 * WebSocket客户端消息处理接口
 * @author majianzheng
 */
public interface MessageHandler {

    void onOpen(Channel channel);

    void onText(String text, Channel channel);

    void onBinary(byte[] bytes, Channel channel);

    void onClose(Channel channel);

    void onError(Channel channel);
}
