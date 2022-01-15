package com.mz.jarboot.core.event;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.common.protocol.CommandRequest;

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
