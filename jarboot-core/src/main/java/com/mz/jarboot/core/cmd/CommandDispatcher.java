package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.*;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandCoreSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Command dispatch, the main loop of the logic.
 * @author majianzheng
 */
@SuppressWarnings("all")
public class CommandDispatcher extends Thread {
    private final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(CommandConst.MAX_COMMAND_BUFFER);

    public CommandDispatcher() {
        this.setDaemon(true);
        this.setName("jarboot.command-dispatcher");
        this.start();
    }

    public void publish(String raw) {
        boolean success = this.queue.offer(raw);
        if (!success) {
            logger.warn("Unable to execute command exceed max buffer size, raw : {}", raw);
        }
    }

    @Override
    public void run() {
        for (;;) {
            try {
                String raw = queue.take();
                execute(raw);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void execute(String raw) {
        CommandRequest request = new CommandRequest();
        CommandCoreSession session = null;
        try {
            request.fromRaw(raw);
            session = EnvironmentContext.registerSession(request.getSessionId());

            CommandType type = request.getCommandType();
            AbstractCommand command = CommandBuilder.build(request, session);

            if (null == command) {
                return;
            }
            switch (type) {
                case USER_PUBLIC:
                    EnvironmentContext.runCommand(command);
                    break;
                case INTERNAL:
                    command.run();
                    break;
                default:
                    logger.debug("开始执行命令：{}, {}", type, request.getCommandLine());
                    break;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            if (null != session) {
                session.end();
            }
        }
    }
}
