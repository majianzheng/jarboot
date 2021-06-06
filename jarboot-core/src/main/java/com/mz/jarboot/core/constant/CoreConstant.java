package com.mz.jarboot.core.constant;

/**
 * jarboot-service向客户端发送的指令定义
 * @author majianzheng
 */
public class CoreConstant {
    public static final String EMPTY_STRING = "";
    public static final String NULL_STRING = "null";
    public static final String LOG_NAME = "jarboot-core";

    //使用websocket发送的最大字节阈值
    //向jarboot-service发送的长度小于该值时使用WebSocket发送，否则通过http
    //为了增大jarboot-service的最大连接数，WebSocket的服务端将接收缓存配置的较小
    public static final int SOCKET_MAX_SEND = 1024 * 4;

    private CoreConstant(){}
}
