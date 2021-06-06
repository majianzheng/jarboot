package com.mz.jarboot.core.session;

import com.mz.jarboot.core.cmd.model.ResultModel;

/**
 * @author majianzheng
 */
public interface CommandSession {

    String getSessionId();

    boolean isRunning();

    void setRunning();

    void ack(String message);

    void console(String text);

    void appendResult(ResultModel resultModel);

    void cancel();

    void end();

    void end(boolean success);

    void end(boolean success, String message);
}
