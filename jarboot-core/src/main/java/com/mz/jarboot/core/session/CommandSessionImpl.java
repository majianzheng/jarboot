package com.mz.jarboot.core.session;

import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.advisor.AdviceListener;
import com.mz.jarboot.core.advisor.AdviceWeaver;
import com.mz.jarboot.core.advisor.JobAware;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.stream.ResultStreamDistributor;
import java.lang.instrument.ClassFileTransformer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implement the process handler.
 * @author majianzheng
 */
public class CommandSessionImpl implements CommandCoreSession {
    private boolean running = false;
    private final String sessionId;
    private final AtomicInteger times = new AtomicInteger();
    private AdviceListener listener = null;
    private ClassFileTransformer transformer;
    private volatile String jobId = CoreConstant.EMPTY_STRING;
    public CommandSessionImpl(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getSessionId() {
        return this.sessionId;
    }

    @Override
    public String getJobId() {
        return jobId;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setRunning() {
        this.running = true;
        times.set(0);
        jobId = UUID.randomUUID().toString();
    }

    @Override
    public void console(String text) {
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.CONSOLE);
        resp.setBody(text);
        resp.setSessionId(this.sessionId);
        ResultStreamDistributor.write(resp);
    }
    
    /**
     * 控制台退格
     */
    @Override
    public void backspace(int i) {
        if (i > 0) {
            CommandResponse resp = new CommandResponse();
            resp.setSuccess(true);
            resp.setResponseType(ResponseType.BACKSPACE);
            resp.setBody(String.valueOf(i));
            resp.setSessionId(this.sessionId);
            ResultStreamDistributor.write(resp);
        }
    }
    
    @Override
    public void backspaceLine(String replacedText) {
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.BACKSPACE_LINE);
        resp.setBody(replacedText);
        resp.setSessionId(this.sessionId);
        ResultStreamDistributor.write(resp);
    }

    @Override
    public void appendResult(ResultModel resultModel) {
        ResultStreamDistributor.appendResult(resultModel, this.sessionId);
    }

    @Override
    public void register(AdviceListener adviceListener, ClassFileTransformer transformer) {
        if (adviceListener instanceof JobAware) {
            JobAware processAware = (JobAware) adviceListener;
            if(processAware.getJobId() == null) {
                processAware.setJobId(this.jobId);
                processAware.setSessionId(this.sessionId);
            }
        }
        this.listener = adviceListener;
        AdviceWeaver.reg(listener);
        this.transformer = transformer;
    }

    @Override
    public AtomicInteger times() {
        return times;
    }

    @Override
    public void cancel() {
        running = false;
        end();
    }

    @Override
    public void end() {
        end(true, CoreConstant.EMPTY_STRING);
    }

    @Override
    public void end(boolean success) {
        end(success, CoreConstant.EMPTY_STRING);
    }

    @Override
    public void end(boolean success, String message) {
        running = false;
        //jobId置为空，以便清理
        jobId = CoreConstant.EMPTY_STRING;
        times.set(0);
        if (transformer != null) {
            EnvironmentContext.getTransformerManager().removeTransformer(transformer);
            this.transformer = null;
        }
        AdviceWeaver.unReg(listener);

        CommandResponse resp = new CommandResponse();
        resp.setSuccess(success);
        resp.setResponseType(ResponseType.COMMAND_END);
        resp.setBody(message);
        resp.setSessionId(this.sessionId);
        ResultStreamDistributor.write(resp);
    }
}
