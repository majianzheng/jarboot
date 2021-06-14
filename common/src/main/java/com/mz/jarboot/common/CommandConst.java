package com.mz.jarboot.common;

public class CommandConst {

    //控制位定义
    public static final char CONSOLE_TYPE = 'c'; //终端消息
    public static final char JSONRESULT_TYPE = 'j'; //终端消息
    public static final char ACK_TYPE = 'k'; //响应命令的ack
    public static final char ONLINE_TYPE = 'o'; //agent上线
    public static final char COMPLETE_TYPE = 'F'; //命令完成消息

    public static final char USER_COMMAND = 'u';
    public static final char INTERNAL_COMMAND = 'i';
    public static final char NONE_COMMAND = 'N';

    //支持的内部命令
    public static final String INVALID_SESSION_CMD = "session_invalid";
    public static final String EXIT_CMD = "exit";
    public static final String CANCEL_CMD = "cancel";

    //广播回话ID，进程退出时，将通知所有浏览器客户端
    public static final String SESSION_COMMON = "common";


    public static final String THREAD_CMD = "thread";
    public static final String THREAD_STACK_CMD = "stack";

    private CommandConst() {}
}
