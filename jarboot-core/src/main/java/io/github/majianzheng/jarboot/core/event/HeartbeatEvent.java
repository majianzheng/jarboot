package io.github.majianzheng.jarboot.core.event;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.common.protocol.CommandRequest;

/**
 * @author jianzhengma
 */
public class HeartbeatEvent implements JarbootEvent {
    private final CommandRequest request;

    public HeartbeatEvent(CommandRequest request) {
        this.request = request;
    }

    public CommandRequest getRequest() {
        return this.request;
    }
}
