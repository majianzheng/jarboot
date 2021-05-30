package com.mz.jarboot.common;

public class CommandRequest implements CmdProtocol {
    private CommandType commandType = CommandType.UNKNOWN;
    private String commandLine = "";

    @Override
    public String toRaw() {
        return this.getCommandTypeChar() + this.commandLine;
    }

    @Override
    public void fromRaw(String raw) {
        if (null == raw || raw.length() < 3) {
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
        commandLine = raw.substring(1);
    }

    @Override
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
}
