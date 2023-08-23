package io.github.majianzheng.jarboot.ws;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.event.SendCommandEvent;

/**
 * @author majianzheng
 */
public class SendCommandSubscriber implements Subscriber<SendCommandEvent> {
    /**
     * Event callback.
     *
     * @param event {@link JarbootEvent}
     */
    @Override
    public void onEvent(SendCommandEvent event) {
        event.send();
    }

    /**
     * subscriber type
     *
     * @return {@link JarbootEvent}
     */
    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return SendCommandEvent.class;
    }
}
