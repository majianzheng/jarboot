package com.mz.jarboot.event;

public enum WsEventEnum {
    NOTICE_INFO,
    NOTICE_WARN,
    NOTICE_ERROR,
    CONSOLE_LINE,  //控制台输出
    RENDER_JSON,
    SERVER_STATUS,  //服务状态改变
    CMD_END,  //命令执行完成
}
