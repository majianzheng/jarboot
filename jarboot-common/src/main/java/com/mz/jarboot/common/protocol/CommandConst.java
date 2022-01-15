package com.mz.jarboot.common.protocol;

/**
 * 命令交互常量定义
 * @author majianzheng
 */
public class CommandConst {
    public static final int MAX_COMMAND_BUFFER = 16384;
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

    /**
     * 广播会话ID，进程启动或退出时，广播所有客户端
     */
    public static final String SESSION_COMMON = "common";

    public static final String ACTION_PROP_NAME_KEY = "name";
    public static final String ACTION_PROP_PARAM_KEY = "param";

    public static final String ACTION_NOTICE_INFO = "INFO";
    public static final String ACTION_NOTICE_WARN = "WARN";
    public static final String ACTION_NOTICE_ERROR = "ERROR";

    public static final int MIN_CMD_LEN = 2;

    /**
     * 协议分隔符
     */
    public static final byte PROTOCOL_SPLIT = '\r';

    private CommandConst() {}
}
