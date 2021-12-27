package com.mz.jarboot.common.protocol;

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
     * Json类型的执行结果
     */
    JSON_RESULT,

    /**
     * 执行执行完成
     */
    COMMAND_END,

    /**
     * 日志记录
     */
    LOG_APPENDER,

    /**
     * 动作，请求jarboot server执行
     */
    ACTION,

    /**
     * 非法的未知类型
     */
    UNKNOWN;

    public char value() {
        return (char)this.ordinal();
    }

    public static ResponseType fromChar(char index) {
        index = (char) (index & ~CommandConst.SUCCESS_FLAG);
        ResponseType[] values = ResponseType.values();
        if (index > values.length - 1) {
            return ResponseType.UNKNOWN;
        }
        return values[index];
    }
}
