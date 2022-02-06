package com.mz.jarboot.core.cmd;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.api.event.Subscriber;
import com.mz.jarboot.core.event.CommandEventBuilder;

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
