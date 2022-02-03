package com.mz.jarboot.client.command;

import com.mz.jarboot.common.utils.JsonUtils;

/**
 * @author majianzheng
 */
public class CommandResult {
    private final String cmd;
    private final boolean success;
    private final String msg;

    CommandResult(String cmd, boolean success, String msg) {
        this.cmd = cmd;
        this.success = success;
        this.msg = msg;
    }

    public String getCmd() {
        return cmd;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMsg() {
        return msg;
    }

    public <T> T getObj(Class<T> cls) {
        return JsonUtils.readValue(msg, cls);
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "cmd='" + cmd + '\'' +
                ", success=" + success +
                ", msg='" + msg + '\'' +
                '}';
    }
}
