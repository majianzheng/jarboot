package io.github.majianzheng.jarboot.client.command;

import io.github.majianzheng.jarboot.client.event.MessageRecvEvent;

/**
 * @author majianzheng
 */
public interface NotifyCallback {
    /**
     * Notify callback
     * @param event message receive event
     */
    void invoke(MessageRecvEvent event);
}
