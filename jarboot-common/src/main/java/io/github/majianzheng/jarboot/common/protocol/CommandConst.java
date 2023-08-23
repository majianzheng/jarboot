package io.github.majianzheng.jarboot.common.protocol;

/**
 * 命令交互常量定义
 * @author majianzheng
 */
public class CommandConst {
    /**
     * 控制位头，char占用1个字节，共8位bit
     * -128 -> 1000 0000
     * 从左到右，第1位表示是否成功，后15位表示响应类型
     */
    public static final byte SUCCESS_FLAG = -128;

    /**
     * 支持的内部命令
     */
    public static final String INVALID_SESSION_CMD = "session_invalid";
    public static final String EXIT_CMD = "exit";
    public static final String CANCEL_CMD = "cancel";
    public static final String HEARTBEAT = "heartbeat";
    public static final String SHUTDOWN = "shutdown";

    public static final int MIN_CMD_LEN = 2;

    /**
     * 协议分隔符
     */
    public static final byte PROTOCOL_SPLIT = '\r';

    private CommandConst() {}
}
