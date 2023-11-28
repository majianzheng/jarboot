package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.event.AbstractMessageEvent;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import org.apache.commons.io.FileUtils;

import javax.websocket.Session;

/**
 * WebSocket消息推送
 * @author majianzheng
 */
public class SessionOperator {
    /** websocket会话 */
    protected Session session;
    protected String userDir;
    private String serviceSidPrefix;
    
    public SessionOperator(Session session) {
        this.session = session;
    }
    public SessionOperator(String userDir, Session session) {
        this.userDir = userDir;
        this.session = session;
        if (StringUtils.isEmpty(userDir)) {
            return;
        }
        String dir = FileUtils.getFile(SettingUtils.getWorkspace(), userDir).getAbsolutePath();
        serviceSidPrefix = String.format("service-%08x%08x", SettingUtils.getUuid().hashCode(), dir.hashCode());
    }

    /**
     * 新消息投递
     * @param msg 消息
     */
    public void newMessage(String msg) {
        publish(new MessageSenderEvent(session, msg));
    }

    /**
     * 新消息投递
     * @param msg 消息
     */
    public void newMessage(byte[] msg) {
        publish(new MessageSenderEvent(session, msg));
    }

    /**
     * 新消息投递
     * @param msg 消息
     */
    public void newMessage(AbstractMessageEvent msg) {
        if (StringUtils.isEmpty(msg.getSid()) || StringUtils.isEmpty(serviceSidPrefix)) {
            newMessage(msg.message());
            return;
        }
        final String servicePrefix = "service-";
        if (msg.getSid().startsWith(servicePrefix)) {
            if (msg.getSid().startsWith(serviceSidPrefix)) {
                newMessage(msg.message());
            }
            return;
        }
        newMessage(msg.message());
    }

    /**
     * 检查会话是否存活
     * @return 是否存活
     */
    public boolean isOpen() {
        return this.session.isOpen();
    }

    /**
     * 关闭
     */
    public void close() {
        try {
            this.session.close();
        } catch (Exception e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    protected void publish(MessageSenderEvent event) {
        NotifyReactor.getInstance().publishEvent(event);
    }
}
