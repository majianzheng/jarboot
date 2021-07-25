/**
 * 定义来自后端的WebSocket事件类型
 * @author majianzheng
 */
export enum MSG_EVENT {
    NOTICE_INFO,
    NOTICE_WARN,
    NOTICE_ERROR,
    CONSOLE_LINE,  //控制台输出
    RENDER_JSON,
    SERVER_STATUS,  //服务状态改变
    CMD_END,  //命令执行完成
}
