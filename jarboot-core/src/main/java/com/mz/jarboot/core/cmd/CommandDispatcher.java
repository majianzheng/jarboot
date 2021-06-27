package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.*;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command dispatch, the main loop of the logic.
 * @author majianzheng
 */
public class CommandDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);

    public void execute(String raw) {
        CommandRequest request = new CommandRequest();
        try {
            request.fromRaw(raw);
            CommandSession session = EnvironmentContext.registerSession(request.getSessionId());

            CommandType type = request.getCommandType();
            Command command = CommandBuilder.build(request, session);

            if (null == command) {
                logger.warn("解析命令错误！");
                return;
            }
            logger.debug("开始执行命令：{}, {}", type, request.getCommandLine());
            switch (type) {
                case USER_PUBLIC:
                    EnvironmentContext.runCommand(command);
                    break;
                case INTERNAL:
                    command.run();
                    break;
                default:
                    //ignore
                    break;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }
}
