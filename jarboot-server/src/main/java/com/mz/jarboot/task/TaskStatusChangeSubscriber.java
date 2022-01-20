package com.mz.jarboot.task;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.event.TaskLifecycleEvent;
import com.mz.jarboot.common.notify.AbstractEventRegistry;
import com.mz.jarboot.utils.MessageUtils;
import com.mz.jarboot.utils.TaskUtils;
import java.util.concurrent.Executor;

/**
 * @author majianzheng
 */
public class TaskStatusChangeSubscriber implements Subscriber<TaskLifecycleEvent> {
    /** Event registry */
    private AbstractEventRegistry eventRegistry;

    public TaskStatusChangeSubscriber(AbstractEventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    /**
     * Event callback.
     *
     * @param event {@link JarbootEvent}
     */
    @Override
    public void onEvent(TaskLifecycleEvent event) {
        MessageUtils.upgradeStatus(event.getSid(), event.getStatus());
        final String topic = eventRegistry
                .createLifecycleTopic(event.getName(), event.getLifecycle());
        eventRegistry.receiveEvent(topic, event);
    }

    /**
     * It is up to the listener to determine whether the callback is asynchronous or synchronous.
     *
     * @return {@link Executor}
     */
    @Override
    public Executor executor() {
        return TaskUtils.getTaskExecutor();
    }

    /**
     * subscriber type
     *
     * @return {@link JarbootEvent}
     */
    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return TaskLifecycleEvent.class;
    }
}
