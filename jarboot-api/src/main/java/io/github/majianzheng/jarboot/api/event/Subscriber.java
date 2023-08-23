package io.github.majianzheng.jarboot.api.event;

import java.util.concurrent.Executor;

/**
 *
 * @param <T>
 * @author majianzheng
 */
public interface Subscriber<T extends JarbootEvent> {
    /**
     * Event callback.
     * @param event {@link JarbootEvent}
     */
    void onEvent(T event);

    /**
     * It is up to the listener to determine whether the callback is asynchronous or synchronous.
     *
     * @return {@link Executor}
     */
    default Executor executor() {
        return null;
    }

    /**
     * subscriber type
     * @return {@link JarbootEvent}
     */
    Class<? extends JarbootEvent> subscribeType();
}
