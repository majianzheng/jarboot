package com.mz.jarboot.core.constant;

/**
 * jarboot-service向客户端发送的指令定义
 * @author majianzheng
 */
public class JarbootCoreConstant {

    public static final String LOG_NAME = "jarboot-core";

    //向jarboot-service发送的长度小于该值时使用WebSocket发送，否则通过http
    //为了增大jarboot-service的最大连接数，WebSocket的服务端将接收缓存配置的较小
    public static final int MAX_WS_SEND = 1024 * 4;

    private JarbootCoreConstant(){}
}
