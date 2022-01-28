package com.mz.jarboot.common.notify;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author majianzheng
 */
@SuppressWarnings({"unchecked", "java:S3740", "rawtypes"})
public class NotifyReactor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<String, EventPublisher> publisherMap = new ConcurrentHashMap<>(16);
    private EventPublisher defaultEventPublisher;

    public static NotifyReactor getInstance() {
        return EventNotifyHolder.INSTANCE;
    }

    /**
     * 发布事件
     * @param event {@link JarbootEvent}
     * @return 是否成功
     */
    public boolean publishEvent(final JarbootEvent event) {
        if (null == event) {
            logger.error("event is null point.");
            return false;
        }
        final String topic = event.getClass().getCanonicalName();
        EventPublisher publisher = publisherMap.getOrDefault(topic, null);
        if (null == publisher) {
            logger.debug("Current has no publisher. topic: {}", topic);
            return false;
        }
        return publisher.publishEvent(event);
    }

    /**
     * 注册订阅者
     * @param subscriber {@link Subscriber}
     */
    public void registerSubscriber(final Subscriber subscriber) {
        registerSubscriber(subscriber, defaultEventPublisher);
    }

    /**
     * 注册订阅者
     * @param subscriber {@link Subscriber}
     * @param publisher {@link EventPublisher}
     */
    public void registerSubscriber(final Subscriber subscriber, final EventPublisher publisher) {
        Class cls = subscriber.subscribeType();
        final String topic = cls.getCanonicalName();
        publisherMap.computeIfAbsent(topic, k -> publisher);
        publisherMap.get(topic).addSubscriber(subscriber);
    }

    /**
     * 反订阅
     * @param subscriber {@link Subscriber}
     */
    public void deregisterSubscriber(final Subscriber subscriber) {
        Class cls = subscriber.subscribeType();
        final String topic = cls.getCanonicalName();
        EventPublisher publisher = publisherMap.getOrDefault(topic, null);
        if (null != publisher) {
            publisher.removeSubscriber(subscriber);
        }
    }

    /**
     * 关闭
     */
    public void shutdown() {
        defaultEventPublisher = null;
        publisherMap.forEach((k, v) -> v.shutdown());
        publisherMap.clear();
    }

    private static class EventNotifyHolder {
        static final NotifyReactor INSTANCE = new NotifyReactor();
    }

    private NotifyReactor() {
        //加载Publisher
        ServiceLoader<EventPublisher> publishers = ServiceLoader.load(EventPublisher.class);
        if (publishers.iterator().hasNext()) {
            defaultEventPublisher = publishers.iterator().next();
        }
        if (null == defaultEventPublisher) {
            defaultEventPublisher = new DefaultPublisher(16384, "jarboot.publisher");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
}
