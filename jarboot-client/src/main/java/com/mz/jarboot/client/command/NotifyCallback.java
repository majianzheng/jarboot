package com.mz.jarboot.client.command;

import com.mz.jarboot.client.event.MessageRecvEvent;

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
