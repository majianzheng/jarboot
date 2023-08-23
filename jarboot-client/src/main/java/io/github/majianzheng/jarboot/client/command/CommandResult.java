package io.github.majianzheng.jarboot.client.command;

import io.github.majianzheng.jarboot.common.utils.JsonUtils;

/**
 * @author majianzheng
 */
public class CommandResult {
    private final String sid;
    private final String cmd;
    private final boolean success;
    private final String msg;

    CommandResult(String sid, String cmd, boolean success, String msg) {
        this.sid = sid;
        this.cmd = cmd;
        this.success = success;
        this.msg = msg;
    }

    /**
     * Get the service id
     * @return service id
     */
    public String getSid() {
        return sid;
    }

    /**
     * Get executed command
     * @return command
     */
    public String getCmd() {
        return cmd;
    }

    /**
     * is success
     * @return success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the command result message.
     * @return message
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Parse json string message to object
     * @param cls object class
     * @param <T> type
     * @return object
     */
    public <T> T getObj(Class<T> cls) {
        return JsonUtils.readValue(msg, cls);
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "sid='" + sid + '\'' +
                ", cmd='" + cmd + '\'' +
                ", success=" + success +
                ", msg='" + msg + '\'' +
                '}';
    }
}
