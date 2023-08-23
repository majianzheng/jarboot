package io.github.majianzheng.jarboot.core.cmd;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.core.event.CommandEventBuilder;

/**
 * 内部命令执行事件订阅
 * @author jianzhengma
 */
public class InternalCommandSubscriber implements Subscriber<CommandEventBuilder.InternalCommandEvent> {
    @Override
    public void onEvent(CommandEventBuilder.InternalCommandEvent event) {
        event.getCommand().run();
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return CommandEventBuilder.InternalCommandEvent.class;
    }
}
