package com.mz.jarboot.core.session;

import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.stream.ResultStreamDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the process handler.
 * @author jianzhengma
 */
public class CommandSessionImpl implements CommandSession {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private final ResultStreamDistributor distributor;
    private boolean running = false;
    private final String sessionId;
    public CommandSessionImpl(String sessionId) {
        this.sessionId = sessionId;
        this.distributor = new ResultStreamDistributor(this.sessionId);
    }

    @Override
    public String getSessionId() {
        return this.sessionId;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setRunning() {
        this.running = true;
    }

    @Override
    public void ack(String message) {
        logger.info("ack>>{}", message);
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
        logger.info("end>>{}", success);
        running = false;
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(success);
        resp.setResponseType(ResponseType.COMPLETE);
        resp.setBody(message);
        resp.setSessionId(this.sessionId);
        distributor.write(resp);
    }
}
