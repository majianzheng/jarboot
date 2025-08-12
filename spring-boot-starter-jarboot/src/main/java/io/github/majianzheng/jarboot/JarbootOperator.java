package io.github.majianzheng.jarboot;

import io.github.majianzheng.jarboot.client.command.CommandResult;
import io.github.majianzheng.jarboot.client.command.NotifyCallback;

import java.util.concurrent.Future;

/**
 * @author majianzheng
 */
public interface JarbootOperator {
    /**
     * 执行命令
     * @param serviceId service id
     * @param cmd command
     * @param callback callback
     * @return command result future
     */
    Future<CommandResult> execute(String serviceId, String cmd, NotifyCallback callback);

    /**
     * 强制取消当前执行的命令
     * @param serviceId service id
     */
    void forceCancel(String serviceId);
}
