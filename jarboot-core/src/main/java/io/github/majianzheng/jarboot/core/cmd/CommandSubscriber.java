package io.github.majianzheng.jarboot.core.cmd;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.event.CommandEventBuilder;

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
        return EnvironmentContext.getScheduledExecutor();
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return CommandEventBuilder.CommandEvent.class;
    }
}
