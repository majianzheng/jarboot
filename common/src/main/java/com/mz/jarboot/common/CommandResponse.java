package com.mz.jarboot.common;

/**
 * We defined a response data structure or protocol used give back the executed result.
 * @author jianzhengma
 */
public class CommandResponse implements CmdProtocol {
    private static final char SUCCESS_FLAG = '1';
    private Boolean success;
    private ResponseType responseType = ResponseType.UNKNOWN;
    private String cmd;
    private String body;
    private CommandType commandType = CommandType.UNKNOWN;  //是否同步

    /**
     * Convert to the raw data used network transmission.
     * @return The data which to send by network.
     */
    @Override
    public String toRaw() {
        //格式: 前4个字符是控制位，+ 命令名 + 空格 + 数据体
        //控制位 0 响应类型 1 指令类型，2 是否成功、3保留填-
        char rt = this.getResponseTypeChar();
        char ct = getCommandTypeChar();
        return new StringBuilder() //响应类型
                .append(rt)
                .append(ct) //命令类型
                .append(Boolean.TRUE.equals(success) ? SUCCESS_FLAG : '0')//是否成功标志
                .append('-') //保留位
                .append(cmd) //命令
                .append(' ') //空格
                .append(body)
                .toString();
    }
    @Override
    public void fromRaw(String raw) {
        this.success = false;
        //反向解析出类实例
        int p = raw.indexOf(' ');
        if (p < 6) {
            this.body = "回复的数据格式错误";
            return;
        }
        //获取响应类型
        switch (raw.charAt(0)) {
            case CommandConst.ACK_TYPE:
                this.setResponseType(ResponseType.ACK);
                break;
            case CommandConst.ONLINE_TYPE:
                this.setResponseType(ResponseType.ONLINE);
                break;
            case CommandConst.CONSOLE_TYPE:
                this.setResponseType(ResponseType.CONSOLE);
                break;
            case CommandConst.COMPLETE_TYPE:
                this.setResponseType(ResponseType.COMPLETE);
                break;
            default:
                this.setResponseType(ResponseType.UNKNOWN);
                break;
        }
        //获取命令类型
        switch (raw.charAt(1)) {
            case CommandConst.USER_COMMAND:
                this.setCommandType(CommandType.USER_PUBLIC);
                break;
            case CommandConst.INTERNAL_COMMAND:
                this.setCommandType(CommandType.INTERNAL);
                break;
            default:
                this.setCommandType(CommandType.UNKNOWN);
                break;
        }
        this.success = SUCCESS_FLAG == raw.charAt(2);
        this.cmd = raw.substring(5, p);
        this.body = raw.substring(p + 1);
    }

    public static CommandResponse createFromRaw(String raw) {
        CommandResponse response = new CommandResponse();
        response.fromRaw(raw);
        return response;
    }
    private char getResponseTypeChar() {
        switch (responseType) {
            case ACK:
                return CommandConst.ACK_TYPE;
            case ONLINE:
                return CommandConst.ONLINE_TYPE;
            case CONSOLE:
                return CommandConst.CONSOLE_TYPE;
            case COMPLETE:
                return CommandConst.COMPLETE_TYPE;
            default:
                break;
        }
        return '-';
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

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }
}
