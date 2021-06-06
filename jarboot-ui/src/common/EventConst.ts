/**
 * 定义来自后端的WebSocket事件类型
 */
export enum MSG_EVENT {
    CONSOLE_LINE,  //控制台输出
    SERVER_STATUS,  //服务状态改变
    CMD_COMPLETE,  //命令执行完成
    NOTICE
}
