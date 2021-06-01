package com.mz.jarboot.core.cmd;

/**
 * @author majianzheng
 */
public interface ProcessHandler {

    boolean isCancel();

    boolean isEnded();

    void ack(String message);

    void console(String text);

    void cancel();

    void end();

    void end(boolean success);

    void end(boolean success, String message);
}
