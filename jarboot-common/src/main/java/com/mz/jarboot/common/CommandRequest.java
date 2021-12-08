package com.mz.jarboot.common;

/**
 * 命令请求
 * @author majianzheng
 */
public class CommandRequest implements CmdProtocol {
    private CommandType commandType = CommandType.UNKNOWN;
    private String commandLine = "";
    private String sessionId;

    @Override
    public String toRaw() {
        return commandType.value() + sessionId + CommandConst.PROTOCOL_SPLIT + this.commandLine;
    }

    @Override
    public void fromRaw(String raw) {
        if (null == raw || raw.length() < CommandConst.MIN_CMD_LEN) {
            return;
        }
        commandType = CommandType.fromChar(raw.charAt(0));
        //从第二个字符到第一个空格，为sessionId
        int p = raw.indexOf(CommandConst.PROTOCOL_SPLIT);
        if (p < CommandConst.MIN_CMD_LEN - 1) {
            throw new JarbootException("协议错误，缺少sessionId参数！");
        }
        sessionId = raw.substring(1, p);
        commandLine = raw.substring(p + 1);
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
