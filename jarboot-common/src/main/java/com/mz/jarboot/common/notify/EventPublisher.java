package com.mz.jarboot.common.notify;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public interface EventPublisher {
    /**
     * 发布事件
     * @param event jarboot事件
     * @return 是否成功
     */
    boolean publishEvent(JarbootEvent event);

    /**
     * Add listener.
     *
     * @param subscriber {@link Subscriber}
     */
    void addSubscriber(Subscriber<? extends JarbootEvent> subscriber);

    /**
     * Remove listener.
     *
     * @param subscriber {@link Subscriber}
     */
    void removeSubscriber(Subscriber<? extends JarbootEvent> subscriber);

    /**
     * Notify listener.
     *
     * @param subscriber {@link Subscriber}
     * @param event      {@link JarbootEvent}
     */
    void notifySubscriber(Subscriber subscriber, JarbootEvent event);

    /**
     * Shutdown publisher
     */
    void shutdown();
}
