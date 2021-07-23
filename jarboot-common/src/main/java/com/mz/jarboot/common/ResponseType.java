package com.mz.jarboot.common;

/**
 * 命令响应类型
 * @author jianzhengma
 */
public enum ResponseType {
    /**
     * 服务上线
     */
    ONLINE,
    /**
     * 命令应答
     */
    ACK,
    /**
     * 控制太消息打印
     */
    CONSOLE,
    /**
     * Json类型的执行结果
     */
    JSON_RESULT,
    /**
     * 执行执行完成
     */
    COMMAND_END,
    /**
     * 非法的未知类型
     */
    UNKNOWN
}
