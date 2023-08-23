package io.github.majianzheng.jarboot.common.protocol;

import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

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
    private int row;
    private int col;


    @Override
    public byte[] toRaw() {
        byte[] buf = null;
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128)) {
            byteStream.write(commandType.value());
            if (StringUtils.isNotEmpty(sessionId)) {
                byteStream.write(sessionId.getBytes(StandardCharsets.UTF_8));
            }
            byteStream.write(CommandConst.PROTOCOL_SPLIT);
            String size = String.format("%d,%d", row, col);
            byteStream.write(size.getBytes(StandardCharsets.UTF_8));
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
        if ((index - 1) > 0) {
            sessionId = new String(raw, 1, index - 1, StandardCharsets.UTF_8);
        }
        int begin = index;
        for (int i = index + 1; i < raw.length; ++i) {
            if (CommandConst.PROTOCOL_SPLIT == raw[i]) {
                index = i;
                break;
            }
        }
        String size = new String(raw, begin + 1, (index - begin - 1), StandardCharsets.UTF_8);
        String[] sizes = size.split(",");
        row = Integer.parseInt(sizes[0]);
        col = Integer.parseInt(sizes[1]);
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

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }
}
