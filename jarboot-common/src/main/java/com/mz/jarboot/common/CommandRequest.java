package com.mz.jarboot.common;

/**
 * 命令请求
 * @author jianzhengma
 */
public class CommandRequest implements CmdProtocol {
    private CommandType commandType = CommandType.UNKNOWN;
    private String commandLine = "";
    private String sessionId;

    private char getCommandTypeChar() {
        if (null == this.getCommandType()) {
            return CommandConst.NONE_COMMAND;
        }
        switch (this.getCommandType()) {
            case USER_PUBLIC:
                return CommandConst.USER_COMMAND;
            case INTERNAL:
                return CommandConst.INTERNAL_COMMAND;
            default:
                break;
        }
        return '-';
    }

    @Override
    public String toRaw() {
        return this.getCommandTypeChar() + sessionId + ' ' + this.commandLine;
    }

    @Override
    public void fromRaw(String raw) {
        if (null == raw || raw.length() < CommandConst.MIN_CMD_LEN) {
            return;
        }
        switch (raw.charAt(0)) {
            case CommandConst.USER_COMMAND:
                commandType = CommandType.USER_PUBLIC;
                break;
            case CommandConst.INTERNAL_COMMAND:
                commandType  = CommandType.INTERNAL;
                break;
            default:
                commandType = CommandType.UNKNOWN;
                break;
        }
        //从第二个字符到第一个空格，为sessionId
        int p = raw.indexOf(' ');
        if (p < CommandConst.MIN_CMD_LEN - 1) {
            throw new MzException("协议错误，缺少sessionId参数！");
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
