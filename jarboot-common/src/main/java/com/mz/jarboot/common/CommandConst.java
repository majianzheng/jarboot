package com.mz.jarboot.common;

/**
 * 命令交互常量定义
 * @author majianzheng
 */
public class CommandConst {
    public static final int MAX_COMMAND_BUFFER = 16384;
    /**
     * 控制位头，char占用2个自己，共16位bit
     * 1000 0000 0000 0001
     * 从左到右，第8位表示是否成功
     */
    private static final char BASE_TYPE = 0x8000;

    public static final char SUCCESS_FLAG = 0x0100;

    /**
     * 终端消息
     */
    public static final char CONSOLE_TYPE = BASE_TYPE + 1;

    /**
     * 程序的标准输出流
     */
    public static final char STD_OUT_TYPE = BASE_TYPE + 2;

    /**
     * Json格式结果
     */
    public static final char JSON_RESULT_TYPE = BASE_TYPE + 3;

    /**
     * agent上线
     */
    public static final char ONLINE_TYPE = BASE_TYPE + 4;

    /**
     * 命令完成消息
     */
    public static final char CMD_END_TYPE = BASE_TYPE + 5;

    /**
     * 发送动作指令
     */
    public static final char ACTION_TYPE = BASE_TYPE + 6;

    public static final char USER_COMMAND = 0xF001;
    public static final char INTERNAL_COMMAND = 0xF002;
    public static final char NONE_COMMAND = 0xFF00;

    /**
     * 支持的内部命令
     */
    public static final String INVALID_SESSION_CMD = "session_invalid";
    public static final String EXIT_CMD = "exit";
    public static final String CANCEL_CMD = "cancel";

    /**
     * 广播会话ID，进程启动或退出时，广播所有客户端
     */
    public static final String SESSION_COMMON = "common";

    public static final String ACTION_PROP_NAME_KEY = "name";
    public static final String ACTION_PROP_PARAM_KEY = "param";

    public static final String ACTION_RESTART = "restart";
    public static final String ACTION_NOTICE_INFO = "INFO";
    public static final String ACTION_NOTICE_WARN = "WARN";
    public static final String ACTION_NOTICE_ERROR = "ERROR";

    public static final int MIN_CMD_LEN = 2;

    /**
     * 协议分隔符
     */
    public static final char PROTOCOL_SPLIT = '\r';

    private CommandConst() {}
}
