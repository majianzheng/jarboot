package com.mz.jarboot.common;

/**
 * @author jianzhengma
 */
public interface CmdProtocol {

    CommandType getCommandType();

    String toRaw();

    void fromRaw(String raw);

    default char getCommandTypeChar() {
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

}
