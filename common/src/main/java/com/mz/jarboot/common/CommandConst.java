package com.mz.jarboot.common;

public class CommandConst {
    /**
     * 命令类型，定义
     */
    public static final char CONSOLE_TYPE = 'c'; //终端消息
    public static final char ACK_TYPE = 'k'; //响应命令的ack
    public static final char ONLINE_TYPE = 'o'; //agent上线
    public static final char COMPLETE_TYPE = 'F'; //终端消息

    public static final char USER_COMMAND = 'u';
    public static final char INTERNAL_COMMAND = 'i';
    public static final char NONE_COMMAND = 'N';

    //支持的命令

    /**
     * 通知退出，进程将尝试优雅退出
     */
    public static final String EXIT_CMD = "exit";

    /**
     *
     */
    public static final String JVM_CMD = "jvm";

    /**
     * 获取目标进程JVM内存信息
     */
    public static final String SYS_PROP_CMD = "sysprop";
    public static final String SYS_ENV_CMD = "sysenv";

    /**
     * 获取目标进程线程信息
     */
    public static final String THREAD_CMD = "thread";

    /**
     * 查看线程调用栈，传入参数为线程id
     */
    public static final String THREAD_STACK_CMD = "stack";

    private CommandConst() {}
}
