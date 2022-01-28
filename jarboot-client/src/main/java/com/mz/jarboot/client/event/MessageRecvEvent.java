package com.mz.jarboot.client.event;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.exception.JarbootRunException;
import com.mz.jarboot.common.notify.FrontEndNotifyEventType;
import com.mz.jarboot.common.protocol.NotifyType;

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

    public String getSid() {
        return sid;
    }

    public FrontEndNotifyEventType getEvent() {
        return event;
    }

    public String getBody() {
        return body;
    }

    public Boolean getSuccess() {
        return success;
    }

    public NotifyType getNotifyType() {
        return notifyType;
    }

    public String getMsg() {
        return msg;
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
