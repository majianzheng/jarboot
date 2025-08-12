package io.github.majianzheng.jarboot.task;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.notify.AbstractEventRegistry;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;

/**
 * @author majianzheng
 */
public class TaskStatusChangeSubscriber implements Subscriber<TaskLifecycleEvent> {
    /** Event registry */
    private final AbstractEventRegistry eventRegistry;

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
        MessageUtils.upgradeStatus(event.getSetting().getSid(), event.getStatus());
        TaskUtils.getTaskExecutor().execute(() -> {
            final String topic = eventRegistry
                    .createTopic(TaskLifecycleEvent.class, event.getSetting().getName(), event.getLifecycle().name());
            eventRegistry.receiveEvent(topic, event);
            eventRegistry.receiveEvent(TaskLifecycleEvent.class.getName(), event);
            if (event.canNotify()) {
                ClusterClientManager.getInstance().notifyToOtherCluster(event);
            }
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
