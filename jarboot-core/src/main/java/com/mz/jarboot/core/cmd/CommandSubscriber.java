package com.mz.jarboot.core.cmd;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.event.CommandEventBuilder;

import java.util.concurrent.Executor;

/**
 * 普通命令事件订阅
 * @author majianzheng
 */
public class CommandSubscriber implements Subscriber<CommandEventBuilder.CommandEvent> {
    @Override
    public void onEvent(CommandEventBuilder.CommandEvent event) {
        EnvironmentContext.runCommand(event.getCommand());
    }

    @Override
    public Executor executor() {
        return EnvironmentContext.getScheduledExecutorService();
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return CommandEventBuilder.CommandEvent.class;
    }
}
