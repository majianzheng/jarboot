package com.mz.jarboot.task;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.api.event.TaskLifecycleEvent;
import com.mz.jarboot.common.notify.AbstractEventRegistry;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.ws.WebSocketManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * @author majianzheng
 */
@Component
public class TaskStatusChangeSubscriber implements Subscriber<TaskLifecycleEvent> {
    @Autowired
    private AbstractEventRegistry eventRegistry;
    /**
     * Event callback.
     *
     * @param event {@link JarbootEvent}
     */
    @Override
    public void onEvent(TaskLifecycleEvent event) {
        WebSocketManager.getInstance().upgradeStatus(event.getSid(), event.getStatus());
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
