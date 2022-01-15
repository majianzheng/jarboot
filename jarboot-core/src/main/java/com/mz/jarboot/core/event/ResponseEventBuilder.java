package com.mz.jarboot.core.event;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.common.protocol.CommandResponse;
import com.mz.jarboot.common.protocol.ResponseType;

/**
 * @author majianzheng
 */
public class ResponseEventBuilder {
    private final CommandResponse response = new CommandResponse();

    public ResponseEventBuilder type(ResponseType type) {
        response.setResponseType(type);
        return this;
    }

    public ResponseEventBuilder body(String body) {
        response.setBody(body);
        return this;
    }

    public ResponseEventBuilder success(boolean success) {
        response.setSuccess(success);
        return this;
    }

    public ResponseEventBuilder session(String id) {
        response.setSessionId(id);
        return this;
    }

    public JarbootEvent build() {
        return response;
    }
}
