package io.github.majianzheng.jarboot.common.notify;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author majianzheng
 */
@SuppressWarnings({"java:S1181", "unchecked", "java:S3740", "rawtypes"})
public class DefaultPublisher extends AbstractEventLoop implements EventPublisher {
    protected static final Logger logger = LoggerFactory.getLogger(DefaultPublisher.class);
    protected final BlockingQueue<JarbootEvent> queue;
    protected final Map<Class<? extends JarbootEvent>, HashSet<Subscriber>> subscribers;

    public DefaultPublisher(int queueSize, String name) {
        super(name);
        queue = new LinkedBlockingQueue<>(queueSize);
        subscribers = new ConcurrentHashMap<>(16);
        this.start();
    }

    @Override
    protected void loop() throws InterruptedException {
        final JarbootEvent event = queue.take();
        receiveEvent(event);
    }

    /**
     * 发布事件
     *
     * @param event jarboot事件
     * @return 是否成功
     */
    @Override
    public boolean publishEvent(JarbootEvent event) {
        return this.queue.offer(event);
    }

    /**
     * Add listener.
     *
     * @param subscriber {@link Subscriber}
     */
    @Override
    public void addSubscriber(Subscriber<? extends JarbootEvent> subscriber) {
        Class<? extends JarbootEvent> cls = subscriber.subscribeType();
        subscribers.compute(cls, (k, v) -> {
            if (null == v) {
                v = new HashSet<>(16);
            }
            v.add(subscriber);
            return v;
        });
    }

    /**
     * Remove listener.
     *
     * @param subscriber {@link Subscriber}
     */
    @Override
    public void removeSubscriber(Subscriber<? extends JarbootEvent> subscriber) {
        Class<? extends JarbootEvent> cls = subscriber.subscribeType();
        subscribers.compute(cls, (k, v) -> {
            if (null != v) {
                v.remove(subscriber);
            }
            return v;
        });
    }

    /**
     * Receive and notifySubscriber to process the event.
     *
     * @param event {@link JarbootEvent}.
     */
    void receiveEvent(JarbootEvent event) {
        final HashSet<Subscriber> subs = subscribers.get(event.getClass());
        if (null == subs || subs.isEmpty()) {
            return;
        }
        subs.forEach(sub -> notifySubscriber(sub, event));
    }

    /**
     * Notify listener.
     *
     * @param subscriber {@link Subscriber}
     * @param event      {@link JarbootEvent}
     */
    @Override
    public void notifySubscriber(Subscriber<JarbootEvent> subscriber, JarbootEvent event) {
        final Runnable job = () -> subscriber.onEvent(event);
        final Executor executor = subscriber.executor();

        if (executor != null) {
            executor.execute(job);
        } else {
            try {
                job.run();
            } catch (Throwable e) {
                logger.error("Event callback exception: ", e);
            }
        }
    }

    /**
     * Shutdown publisher
     */
    @Override
    public void shutdown() {
        this.shutdown = true;
        this.interrupt();
        this.queue.clear();
    }
}
