/**
 * 定义来自后端的WebSocket事件类型
 * @author majianzheng
 */
export enum MSG_EVENT {
    NOTICE,
    CONSOLE_LINE,  //控制台输出
    CONSOLE_PRINT,  //控印制台打字符串（不换行）
    BACKSPACE,  //控制台退格
    RENDER_JSON,
    SERVER_STATUS, //服务状态改变
    CMD_END,       //命令执行完成
    WORKSPACE_CHANGE, //工作空间变更
    JVM_PROCESS_CHANGE, //java进程变化事件
    GLOBAL_LOADING, //全局Loading提示
}
