package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.common.protocol.NotifyType;

/**
 * @author majianzheng
 */
public class BroadcastMessageEvent extends AbstractMessageEvent {
    public BroadcastMessageEvent(String sid) {
        this.sid = sid;
    }

    public BroadcastMessageEvent body(String body) {
        this.body = body;
        return this;
    }

    public BroadcastMessageEvent body(String text, NotifyType level) {
        this.noticeBody(text, level);
        return this;
    }

    public BroadcastMessageEvent type(FrontEndNotifyEventType type) {
        this.type = type;
        return this;
    }
}
