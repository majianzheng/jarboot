package io.github.majianzheng.jarboot.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.ws.MessageSenderEvent;

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
