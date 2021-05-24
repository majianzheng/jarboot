package com.mz.jarboot.common;

/**
 * 收到的消息格式
 * @author majianzheng
 */
public class Command {
    //命令号
    private String cmd;
    //命令参数
    private String param;
    //是否需要反馈，暂不考虑丢失、重发问题，超时便丢弃
    private Boolean ack;

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

    public Boolean getAck() {
        return ack;
    }

    public void setAck(Boolean ack) {
        this.ack = ack;
    }
}
