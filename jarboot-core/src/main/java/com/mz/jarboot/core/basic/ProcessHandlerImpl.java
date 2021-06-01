package com.mz.jarboot.core.basic;

import com.mz.jarboot.common.CommandResponse;
import com.mz.jarboot.common.ResponseType;
import com.mz.jarboot.core.cmd.ProcessHandler;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.resp.RespStreamStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implement the process handler.
 * @author jianzhengma
 */
public class ProcessHandlerImpl implements ProcessHandler {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static RespStreamStrategy respStreamStrategy = new RespStreamStrategy();
    private AtomicBoolean canceled = new AtomicBoolean(false);
    private AtomicBoolean ended = new AtomicBoolean(false);
    private String command;
    public ProcessHandlerImpl(String name) {
        this.command = name;
    }
    @Override
    public boolean isCancel() {
        return canceled.get();
    }

    @Override
    public boolean isEnded() {
        return ended.get();
    }

    @Override
    public void ack(String message) {
        logger.info("ack>>{}", message);
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.ACK);
        resp.setBody(message);
        resp.setCmd(command);
        logger.info("write>>");
        respStreamStrategy.write(resp);
    }

    @Override
    public void console(String text) {
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(true);
        resp.setResponseType(ResponseType.CONSOLE);
        resp.setBody(text);
        resp.setCmd(command);
        respStreamStrategy.write(resp);
    }

    @Override
    public void cancel() {
        canceled.set(true);
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
        logger.info("end>>{}, {}", command, success);
        ended.set(true);
        CommandResponse resp = new CommandResponse();
        resp.setSuccess(success);
        resp.setResponseType(ResponseType.COMPLETE);
        resp.setBody(message);
        resp.setCmd(command);
        respStreamStrategy.write(resp);
    }
}
