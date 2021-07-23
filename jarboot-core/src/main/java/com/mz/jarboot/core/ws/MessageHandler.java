package com.mz.jarboot.core.ws;

import io.netty.channel.Channel;

/**
 * WebSocket客户端消息处理接口
 * @author majianzheng
 */
public interface MessageHandler {

    /**
     * WebSocket 连接成功
     * @param channel 通道
     */
    void onOpen(Channel channel);

    /**
     * 新字符串消息
     * @param text     消息
     * @param channel  通道
     */
    void onText(String text, Channel channel);

    /**
     * 新字节码消息
     * @param bytes    字节码消息
     * @param channel  通道
     */
    void onBinary(byte[] bytes, Channel channel);

    /**
     * 连接关闭
     * @param channel 通道
     */
    void onClose(Channel channel);

    /**
     * 连接异常
     * @param channel 通道
     */
    void onError(Channel channel);
}
