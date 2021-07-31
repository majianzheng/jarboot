package com.mz.jarboot.api.cmd.session;


/**
 * @author majianzheng
 */
public interface CommandSession {

    /**
     * 获取会话id
     * @return 会话id
     */
    String getSessionId();

    /**
     * 每执行一次命令生成一个唯一id
     * @return Job uuid
     */
    String getJobId();

    /**
     * 控制台消息打印
     * @param text 消息
     */
    void console(String text);

    /**
     * 取消执行
     */
    void cancel();

    /**
     * 结束
     */
    void end();

    /**
     * 结束
     * @param success 是否成功
     */
    void end(boolean success);

    /**
     * 结束
     * @param success 是否成功
     * @param message 消息
     */
    void end(boolean success, String message);
}
