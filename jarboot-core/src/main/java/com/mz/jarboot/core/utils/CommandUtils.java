package com.mz.jarboot.core.utils;

import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.session.ExitStatus;

/**
 * Command Process util
 * 以下代码基于开源项目Arthas适配修改
 */
public class CommandUtils {

    /**
     * check exit status and end command processing
     * @param process CommandProcess instance
     * @param status ExitStatus of command
     */
    public static void end(CommandSession process, ExitStatus status) {
        if (status != null) {
            process.end(0 == status.getStatusCode(), status.getMessage());
        } else {
            process.end(false, "process error, exit status is null");
        }
    }

    private CommandUtils() {}
}
