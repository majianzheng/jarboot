package com.mz.jarboot.core.msg;

import com.mz.jarboot.common.CommandResponse;

/**
 * 构建CommandResponse
 * @author jianzhengma
 */
public class ResponseBuilder {
    private CommandResponse response = new CommandResponse();

    public CommandResponse getResponse() {
        return response;
    }
    public ResponseBuilder setType(String type) {
        response.setType(type);
        return this;
    }
    public ResponseBuilder setCmd(String cmd) {
        response.setCmd(cmd);
        return this;
    }
    public ResponseBuilder setBody(String body) {
        response.setBody(body);
        return this;
    }
}
