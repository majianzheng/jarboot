package com.mz.jarboot.core.session;

import com.mz.jarboot.core.advisor.AdviceListener;
import com.mz.jarboot.core.cmd.model.ResultModel;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author majianzheng
 */
public interface CommandSession {

    String getSessionId();

    /**
     * 每执行一次命令生成一个唯一id
     * @return job id
     */
    String getJobId();

    boolean isRunning();

    void setRunning();

    void ack(String message);

    void console(String text);

    void appendResult(ResultModel resultModel);

    void register(AdviceListener adviceListener, ClassFileTransformer transformer);

    AtomicInteger times();

    void cancel();

    void end();

    void end(boolean success);

    void end(boolean success, String message);
}
