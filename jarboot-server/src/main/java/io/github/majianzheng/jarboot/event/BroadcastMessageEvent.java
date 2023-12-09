package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.common.notify.FrontEndNotifyEventType;
import io.github.majianzheng.jarboot.common.protocol.NotifyType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author majianzheng
 */
public class BroadcastMessageEvent extends AbstractMessageEvent {
    private final Set<String> sessionIds = new HashSet<>(16);
    public BroadcastMessageEvent(String sid) {
        this.sid = sid;
    }

    public BroadcastMessageEvent(String sessionIds, String sid) {
        this.sid = sid;
        this.sessionIds.addAll(Arrays.asList(sessionIds.split(",")));
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

    public Set<String> getSessionIds() {
        return sessionIds;
    }
}
