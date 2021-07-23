package com.mz.jarboot.common;

/**
 * 命令交互常量定义
 * @author jianzhengma
 */
public class CommandConst {

    /**
     * 控制位定义
     */

    /**
     * 终端消息
     */
    public static final char CONSOLE_TYPE = 'c';
    /**
     * 终端消息
     */
    public static final char JSON_RESULT_TYPE = 'j';
    /**
     * 响应命令的ack
     */
    public static final char ACK_TYPE = 'k';
    /**
     * agent上线
     */
    public static final char ONLINE_TYPE = 'o';
    /**
     * 命令完成消息
     */
    public static final char CMD_END_TYPE = 'F';

    public static final char USER_COMMAND = 'u';
    public static final char INTERNAL_COMMAND = 'i';
    public static final char NONE_COMMAND = 'N';

    /**
     * 支持的内部命令
     */
    public static final String INVALID_SESSION_CMD = "session_invalid";
    public static final String EXIT_CMD = "exit";
    public static final String CANCEL_CMD = "cancel";

    /**
     * 广播回话ID，进程退出时，将通知所有浏览器客户端
     */
    public static final String SESSION_COMMON = "common";


    public static final int MIN_CMD_LEN = 3;

    private CommandConst() {}
}
