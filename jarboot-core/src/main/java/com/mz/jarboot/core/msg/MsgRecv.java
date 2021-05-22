package com.mz.jarboot.core.msg;

/**
 * 收到的消息格式
 * @author majianzheng
 */
public class MsgRecv {
    private String cmd;
    private String param;

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
