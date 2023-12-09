package io.github.majianzheng.jarboot.common.protocol;

/**
 * 命令类型
 * @author majianzheng
 */
public enum CommandType {
    /**
     * 用户命令，前端或api发起的执行
     */
    USER_PUBLIC,

    /**
     * 系统内部的控制指令
     */
    INTERNAL,

    /**
     * 不支持的未知命令
     */
    UNKNOWN;

    public byte value() {
        return (byte)this.ordinal();
    }

    public static CommandType fromChar(byte index) {
        CommandType[] values = CommandType.values();
        if (index > values.length - 1) {
            return CommandType.UNKNOWN;
        }
        return values[index];
    }
}
