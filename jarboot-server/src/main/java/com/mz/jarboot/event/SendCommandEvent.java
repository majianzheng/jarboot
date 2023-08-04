package com.mz.jarboot.event;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.ws.MessageSenderEvent;

/**
 * 向Agent发送命令
 * @author mazheng
 */
public class SendCommandEvent implements JarbootEvent {
    private final MessageSenderEvent messageSenderEvent;
    public SendCommandEvent(MessageSenderEvent event) {
        this.messageSenderEvent = event;
    }
    public void send() {
        messageSenderEvent.send();
    }
}
