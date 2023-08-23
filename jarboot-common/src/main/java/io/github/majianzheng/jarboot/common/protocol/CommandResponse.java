package io.github.majianzheng.jarboot.common.protocol;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * We defined a response data structure or protocol used give back the executed result.
 * @author majianzheng
 */
public class CommandResponse implements CmdProtocol, JarbootEvent {
    private Boolean success;
    private ResponseType responseType = ResponseType.UNKNOWN;
    private String body;
    private String sessionId;

    /**
     * Convert to the raw data used network transmission.
     * @return The data which to send by network.
     */
    @Override
    public byte[] toRaw() {
        //控制位 0 响应类型、1 是否成功、2保留填-
        byte cb = this.responseType.value();
        if (Boolean.TRUE.equals(success)) {
            cb = (byte)(cb | CommandConst.SUCCESS_FLAG);
        }
        byte[] buf = null;
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024)) {
            byteStream.write(cb);
            byteStream.write(body.getBytes(StandardCharsets.UTF_8));
            byteStream.write(CommandConst.PROTOCOL_SPLIT);
            if (StringUtils.isNotEmpty(sessionId)) {
                byteStream.write(sessionId.getBytes(StandardCharsets.UTF_8));
            }
            buf = byteStream.toByteArray();
        } catch (IOException e) {
            //ignore
        }
        return buf;
    }
    @Override
    public void fromRaw(byte[] raw) {
        byte h = raw[0];
        this.success = CommandConst.SUCCESS_FLAG == (CommandConst.SUCCESS_FLAG & h);
        //取反再与得到真实响应类型
        this.responseType = ResponseType.fromChar(h);
        int index = -1;
        for (int i = raw.length - 1; i > 0; --i) {
            if (CommandConst.PROTOCOL_SPLIT == raw[i]) {
                index = i;
                break;
            }
        }
        if (-1 == index) {
            this.success = false;
            this.body = "协议错误，未发现sessionId";
            return;
        }
        this.body = new String(raw, 1, index - 1, StandardCharsets.UTF_8);
        final int len = (raw.length - index - 1);
        if (len > 0) {
            this.sessionId = new String(raw, index + 1, len, StandardCharsets.UTF_8);
        }
    }

    public static CommandResponse createFromRaw(byte[] raw) {
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
