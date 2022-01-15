package com.mz.jarboot.event;

/**
 * 往前端推送事件类型
 * @author majianzheng
 */

public enum FrontEndNotifyEventType {
    /**
     * 弹出提示
     */
    NOTICE,

    /**
     * 控制台输出
     */
    CONSOLE,
    
    /**
     * 控制台打印字符串
     */
    STD_PRINT,
    
    /**
     * 控制台退格
     */
    BACKSPACE,

    /**
     * 渲染Json
     */
    RENDER_JSON,

    /**
     * 服务状态改变
     */
    SERVER_STATUS,

    /**
     * 命令执行完成
     */
    CMD_END,

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
