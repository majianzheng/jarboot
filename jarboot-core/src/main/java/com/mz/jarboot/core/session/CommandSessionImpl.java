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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implement the process handler.
 * @author jianzhengma
 */
public class CommandSessionImpl implements CommandSession {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private final ResultStreamDistributor distributor;
    private boolean running = false;
    private final String sessionId;
    private final AtomicInteger times = new AtomicInteger();
    private AdviceListener listener = null;
    private ClassFileTransformer transformer;
    private volatile String jobId = CoreConstant.EMPTY_STRING; //NOSONAR
    public CommandSessionImpl(String sessionId) {
        this.sessionId = sessionId;
        this.distributor = new ResultStreamDistributor(this.sessionId);
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
    public void ack(String message) {
        logger.debug("ack>>{}", message);
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.ACK);
        resp.setBody(message);
        resp.setSessionId(this.sessionId);
        logger.info("write>>");
        distributor.write(resp);
    }

    @Override
    public void console(String text) {
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.CONSOLE);
        resp.setBody(text);
        resp.setSessionId(this.sessionId);
        distributor.write(resp);
    }

    @Override
    public void appendResult(ResultModel resultModel) {
        distributor.appendResult(resultModel);
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
        end(true, null);
    }

    @Override
    public void end(boolean success) {
        end(success, null);
    }

    @Override
    public void end(boolean success, String message) {
        logger.debug("end>>{}, {}", success, message);
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
        resp.setResponseType(ResponseType.COMPLETE);
        resp.setBody(message);
        resp.setSessionId(this.sessionId);
        distributor.write(resp);
    }
}
