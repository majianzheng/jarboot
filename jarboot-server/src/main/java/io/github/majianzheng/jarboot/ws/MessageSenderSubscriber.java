package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;

/**
 * @author majianzheng
 */
public class MessageSenderSubscriber implements Subscriber<MessageSenderEvent> {
    /**
     * Event callback.
     *
     * @param event {@link JarbootEvent}
     */
    @Override
    public void onEvent(MessageSenderEvent event) {
        event.send();
    }

    /**
     * subscriber type
     *
     * @return {@link JarbootEvent}
     */
    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return MessageSenderEvent.class;
    }
}
