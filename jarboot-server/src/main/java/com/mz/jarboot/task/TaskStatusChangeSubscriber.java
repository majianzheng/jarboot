package com.mz.jarboot.task;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.event.TaskLifecycleEvent;
import com.mz.jarboot.common.notify.AbstractEventRegistry;
import com.mz.jarboot.utils.MessageUtils;
import com.mz.jarboot.utils.TaskUtils;

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
        TaskUtils.getTaskExecutor().execute(() -> {
            final String topic = eventRegistry
                    .createTopic(TaskLifecycleEvent.class, event.getName(), event.getLifecycle().name());
            eventRegistry.receiveEvent(topic, event);
        });
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
