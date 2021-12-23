package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.common.protocol.CommandRequest;
import com.mz.jarboot.common.protocol.CommandType;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.session.CommandCoreSession;
import com.mz.jarboot.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Command dispatch, the main loop of the logic.
 * @author majianzheng
 */
public class CommandDispatcher extends Thread {
    private final Logger logger = LogUtils.getLogger();

    /** 消息处理队列 */
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(CommandConst.MAX_COMMAND_BUFFER);
    /** 心跳响应 */
    private final Runnable heartbeat;

    public CommandDispatcher(Runnable heartbeat) {
        this.heartbeat = heartbeat;
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
                logger.info(e.getMessage(), e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
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
            switch (type) {
                case USER_PUBLIC:
                    EnvironmentContext.runCommand(CommandBuilder.build(request, session));
                    break;
                case INTERNAL:
                    execInternalCmd(CommandBuilder.build(request, session));
                    break;
                case HEARTBEAT:
                    this.heartbeat.run();
                    break;
                default:
                    logger.debug("未知类型的命令：{}, {}", type, request.getCommandLine());
                    break;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (null != session) {
                session.end();
            }
        }
    }

    private void execInternalCmd(AbstractCommand command) {
        if (null == command) {
            return;
        }
        command.run();
    }
}
