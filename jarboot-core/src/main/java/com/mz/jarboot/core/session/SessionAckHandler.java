package com.mz.jarboot.core.session;

/**
 * @author jianzhengma
 */
public interface SessionAckHandler {
    /**
     * 应答
     * @param message 消息
     */
    void ack(String message);
}
