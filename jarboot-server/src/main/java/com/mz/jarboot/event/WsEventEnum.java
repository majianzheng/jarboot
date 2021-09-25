package com.mz.jarboot.event;

/**
 * @author majianzheng
 */

public enum WsEventEnum {
    /**
     * 提示通知
     */
    NOTICE_INFO,

    /**
     * 警告通知
     */
    NOTICE_WARN,

    /**
     * 错误通知
     */
    NOTICE_ERROR,

    /**
     * 控制台输出
     */
    CONSOLE_LINE,

    /**
     * 控制台后退一行
     */
    BACKSPACE_LINE,

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
}
