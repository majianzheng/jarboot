package com.mz.jarboot.common;

public class CommandConst {
    /**
     * 通知退出，进程将尝试优雅退出
     */
    public static final String EXIT_CMD = "exit";

    /**
     * 获取目标进程JVM内存信息
     */
    public static final String GET_MEM_INFO_CMD = "memory";

    /**
     * 获取目标进程线程信息
     */
    public static final String THREAD_CMD = "thread";

    /**
     * 查看线程调用栈，传入参数为线程id
     */
    public static final String THREAD_STACK_CMD = "stack";
}
