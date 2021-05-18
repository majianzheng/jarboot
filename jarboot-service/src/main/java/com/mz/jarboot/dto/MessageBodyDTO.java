package com.mz.jarboot.dto;

public class MessageBodyDTO {
    //"{\"server\":\"%s\",\"serverType\":\"%s\",\"msgType\":\"%s\",\"text\":\"%s\"}"
    private String server;
    private String serverType;
    private String msgType;
    private String text;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
