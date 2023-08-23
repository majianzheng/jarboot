package io.github.majianzheng.jarboot.common.notify;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;

/**
 * @author majianzheng
 */
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
    void notifySubscriber(Subscriber<JarbootEvent> subscriber, JarbootEvent event);

    /**
     * Shutdown publisher
     */
    void shutdown();
}
