package com.mz.jarboot.common.protocol;

/**
 * We defined a response data structure or protocol used give back the executed result.
 * @author majianzheng
 */
public class CommandResponse implements CmdProtocol {
    private Boolean success;
    private ResponseType responseType = ResponseType.UNKNOWN;
    private String body;
    private String sessionId;

    /**
     * Convert to the raw data used network transmission.
     * @return The data which to send by network.
     */
    @Override
    public String toRaw() {
        //控制位 0 响应类型、1 是否成功、2保留填-
        char cb = this.responseType.value();
        if (Boolean.TRUE.equals(success)) {
            cb = (char)(cb | CommandConst.SUCCESS_FLAG);
        }
        if (null == sessionId || sessionId.isEmpty()) {
            sessionId = CommandConst.SESSION_COMMON;
        }
        return new StringBuilder()
                .append(cb)
                .append(body)
                .append(CommandConst.PROTOCOL_SPLIT)
                .append(sessionId)
                .toString();
    }
    @Override
    public void fromRaw(String raw) {
        char h = raw.charAt(0);
        this.success = CommandConst.SUCCESS_FLAG == (CommandConst.SUCCESS_FLAG & h);
        //取反再与得到真实响应类型
        this.responseType = ResponseType.fromChar(h);
        int index = raw.lastIndexOf(CommandConst.PROTOCOL_SPLIT);
        if (-1 == index) {
            this.success = false;
            this.body = "协议错误，未发现sessionId";
            return;
        }
        this.body = raw.substring(1, index);
        this.sessionId = raw.substring(index + 1);
    }

    public static CommandResponse createFromRaw(String raw) {
        CommandResponse response = new CommandResponse();
        response.fromRaw(raw);
        return response;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
