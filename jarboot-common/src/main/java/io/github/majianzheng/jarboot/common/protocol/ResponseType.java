package io.github.majianzheng.jarboot.common.protocol;

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
     * 日志记录
     */
    LOG_APPENDER,

    /**
     * Notice
     */
    NOTIFY,

    /**
     * 非法的未知类型
     */
    UNKNOWN;

    public byte value() {
        return (byte)this.ordinal();
    }

    public static ResponseType fromChar(byte index) {
        index = (byte) (index & ~CommandConst.SUCCESS_FLAG);
        ResponseType[] values = ResponseType.values();
        if (index > values.length - 1) {
            return ResponseType.UNKNOWN;
        }
        return values[index];
    }
}
