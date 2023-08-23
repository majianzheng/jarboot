package io.github.majianzheng.jarboot.client.command;

/**
 * @author majianzheng
 */
public interface MessageListener {
    /**
     * 连接打开
     */
    void onOpen();

    /**
     * 消息接收事件
     * @param text 消息内容
     */
    void onMessage(String text);

    /**
     * 连接关闭
     */
    void onClose();
}
