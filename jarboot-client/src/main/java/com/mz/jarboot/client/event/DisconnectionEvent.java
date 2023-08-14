package com.mz.jarboot.client.event;

import com.mz.jarboot.api.event.JarbootEvent;

/**
 * 与Jarboot服务端连接断开事件
 * 使用{@link com.mz.jarboot.common.notify.NotifyReactor}订阅该事件
 * 事件订阅见 {@link com.mz.jarboot.api.event.Subscriber}
 * @author majianzheng
 */
public class DisconnectionEvent implements JarbootEvent {
    private String user;
    private String host;

    public DisconnectionEvent() {

    }

    public DisconnectionEvent(String host, String user) {
        this.host = host;
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    @Override
    public String toString() {
        return "DisconnectionEvent{" +
                "user='" + user + '\'' +
                ", host='" + host + '\'' +
                '}';
    }
}
