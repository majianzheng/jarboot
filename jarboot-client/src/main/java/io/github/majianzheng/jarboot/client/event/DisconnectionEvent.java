package io.github.majianzheng.jarboot.client.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;

/**
 * 与Jarboot服务端连接断开事件
 * 使用{@link NotifyReactor}订阅该事件
 * 事件订阅见 {@link Subscriber}
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
