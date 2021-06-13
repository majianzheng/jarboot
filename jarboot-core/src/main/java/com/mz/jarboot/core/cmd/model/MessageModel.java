package com.mz.jarboot.core.cmd.model;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class MessageModel extends ResultModel {
    private String message;

    public MessageModel() {
    }

    public MessageModel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getName() {
        return "message";
    }
}
