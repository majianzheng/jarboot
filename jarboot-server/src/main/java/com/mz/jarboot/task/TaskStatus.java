package com.mz.jarboot.task;

/**
 * @author majianzheng
 */

public enum TaskStatus {
    /**
     * 正在启动
     */
    STARTING,

    /**
     * 正在运行
     */
    RUNNING,

    /**
     * 正在停止
     */
    STOPPING,

    /**
     * 已停止
     */
    STOPPED
}
