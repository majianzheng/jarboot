package com.mz.jarboot.event;

/**
 * 往前端推送事件类型
 * @author majianzheng
 */

public enum FrontEndNotifyEventType {
    /**
     * 弹出提示
     */
    NOTIFY,
    
    /**
     * 控制台打印字符串
     */
    STD_PRINT,

    /**
     * 控制台退格
     */
    BACKSPACE,

    /**
     * 服务状态改变
     */
    SERVER_STATUS,

    /**
     * 工作空间变更
     */
    WORKSPACE_CHANGE,

    /**
     * java进程变化
     */
    JVM_PROCESS_CHANGE,

    /**
     * 全局Loading提示
     */
    GLOBAL_LOADING,
}
