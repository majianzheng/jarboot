package com.mz.jarboot.core.cmd;

/**
 * inactive to the ui.
 * @author majianzheng
 */
public interface ProcessHandler {

    boolean isCancel();

    boolean isEnded();

    void ack(String cmd, String message);

    void console(String cmd, String text);

    void cancel(String cmd);

    void end(String cmd);

    void end(String cmd, boolean success);

    void end(String cmd, boolean success, String message);
}
