package com.mz.jarboot.common.protocol;

import com.mz.jarboot.api.event.JarbootEvent;
import com.mz.jarboot.common.JarbootException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 命令请求
 * @author majianzheng
 */
public class CommandRequest implements CmdProtocol, JarbootEvent {
    private CommandType commandType = CommandType.UNKNOWN;
    private String commandLine = "";
    private String sessionId;

    @Override
    public byte[] toRaw() {
        byte[] buf = null;
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128)) {
            byteStream.write(commandType.value());
            byteStream.write(sessionId.getBytes(StandardCharsets.UTF_8));
            byteStream.write(CommandConst.PROTOCOL_SPLIT);
            byteStream.write(commandLine.getBytes(StandardCharsets.UTF_8));
            buf = byteStream.toByteArray();
        } catch (IOException e) {
            //ignore
        }
        return buf;
    }

    @Override
    public void fromRaw(byte[] raw) {
        if (null == raw || raw.length < CommandConst.MIN_CMD_LEN) {
            return;
        }
        commandType = CommandType.fromChar(raw[0]);
        //从第二个字符到第一个空格，为sessionId
        int index = -1;
        for (int i = 1; i < raw.length; ++i) {
            if (CommandConst.PROTOCOL_SPLIT == raw[i]) {
                index = i;
                break;
            }
        }
        if (index < CommandConst.MIN_CMD_LEN - 1) {
            throw new JarbootException("协议错误，缺少sessionId参数！");
        }
        sessionId = new String(raw, 1, index - 1, StandardCharsets.UTF_8);
        commandLine = new String(raw, index + 1, raw.length - index - 1, StandardCharsets.UTF_8);
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
