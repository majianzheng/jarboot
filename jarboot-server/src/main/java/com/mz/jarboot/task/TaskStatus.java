package com.mz.jarboot.task;

/**
 * @author jianzhengma
 */

public enum TaskStatus {
    /**
     * 开始
     */
    START,
    /**
     * 已开始
     */
    STARTED,
    /**
     * 停止
     */
    STOP,
    /**
     * 已停止
     */
    STOPPED,
    /**
     * 启动错误
     */
    START_ERROR,
    /**
     * 停止错误
     */
    STOP_ERROR
}
