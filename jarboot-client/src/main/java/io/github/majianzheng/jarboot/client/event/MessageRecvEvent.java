package io.github.majianzheng.jarboot.client.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.common.protocol.NotifyType;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;

/**
 * 来自服务端的MessageEvent和BroadcastMessageEvent事件接收
 * @author majianzheng
 */
public class MessageRecvEvent implements JarbootEvent {
    private final String sid;
    private final FrontEndNotifyEventType event;
    private final String body;
    private Boolean success = true;
    private NotifyType notifyType;
    private String msg;
    /**
     * 协议格式：sid CR type CR body
     * @param buf 消息
     */
    public MessageRecvEvent(String buf) {
        final char cr = '\r';
        int i = buf.indexOf(cr);
        if (-1 == i) {
            throw new JarbootRunException("协议错误:" + buf);
        }
        sid = 0 == i ? "" : buf.substring(0, i);
        int k = buf.indexOf(cr, i + 1);
        if (-1 == k) {
            throw new JarbootRunException("协议错误，类型为空:" + buf);
        }
        int index = Integer.parseInt(buf.substring(i + 1, k));
        this.event = FrontEndNotifyEventType.values()[index];
        body = buf.substring(k + 1);
        if (FrontEndNotifyEventType.NOTIFY.equals(this.event)) {
            success = '0' == body.charAt(0);
            index = body.indexOf(',');
            int notifyTypeIndex = Integer.parseInt(body.substring(1, index));
            notifyType = NotifyType.values()[notifyTypeIndex];
            msg = body.substring(index + 1);
        }
    }

    /**
     * Get the service id
     * @return service id
     */
    public String getSid() {
        return sid;
    }

    /**
     * Get event type
     * @return event type
     */
    public FrontEndNotifyEventType getEvent() {
        return event;
    }

    /**
     * Get body
     * @return body
     */
    public String getBody() {
        return body;
    }

    /**
     * Get success
     * @return success
     */
    public Boolean getSuccess() {
        return success;
    }

    /**
     * Get notify type
     * @return notify type
     */
    public NotifyType getNotifyType() {
        return notifyType;
    }

    /**
     * Get message
     * @return message
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Parse json string message to object
     * @param cls object class
     * @param <T> type
     * @return object
     */
    public <T> T getMessageObj(Class<T> cls) {
        return JsonUtils.readValue(msg, cls);
    }

    @Override
    public String toString() {
        return "MessageRecvEvent{" +
                "sid='" + sid + '\'' +
                ", event=" + event +
                ", body='" + body + '\'' +
                ", success=" + success +
                ", notifyType=" + notifyType +
                ", msg='" + msg + '\'' +
                '}';
    }
}
