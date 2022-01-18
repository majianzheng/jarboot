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
    private String password;
    private String host;
    private String version;

    public DisconnectionEvent() {

    }

    public DisconnectionEvent(String host, String user, String password, String version) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.version = version;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DisconnectionEvent{" +
                "user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", host='" + host + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
