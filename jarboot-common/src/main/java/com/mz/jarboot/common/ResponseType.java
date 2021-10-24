package com.mz.jarboot.common;

/**
 * 命令响应类型
 * @author majianzheng
 */
public enum ResponseType {
    /**
     * 心跳
     */
    HEARTBEAT,

    /**
     * 程序中的标准输出流
     */
    STD_PRINT,

    /**
     * 控制台消息打印
     */
    CONSOLE,
    
    /**
     * 控制台退格
     */
    BACKSPACE,

    /**
     * 控制台后退一行
     */
    BACKSPACE_LINE,

    /**
     * Json类型的执行结果
     */
    JSON_RESULT,

    /**
     * 执行执行完成
     */
    COMMAND_END,

    /**
     * 动作，请求jarboot server执行
     */
    ACTION,

    /**
     * 非法的未知类型
     */
    UNKNOWN
}
