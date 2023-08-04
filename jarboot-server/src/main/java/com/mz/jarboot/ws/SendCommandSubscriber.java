package com.mz.jarboot.ws;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.event.SendCommandEvent;

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
