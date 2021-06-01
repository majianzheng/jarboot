package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.*;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.basic.ProcessHandlerImpl;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command dispatch, the main loop of the logic.
 * @author majianzheng
 */
public class CommandDispatch {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);

    public void execute(String raw) {
        logger.debug("收到消息：{}", raw);
        CommandRequest request = new CommandRequest();
        ProcessHandler processHandler = new ProcessHandlerImpl("none");
        try {
            request.fromRaw(raw);
            CommandType type = request.getCommandType();
            Command command = CommandBuilder.build(type, request.getCommandLine());
            if (null == command) {
                logger.warn("解析命令错误！");
                processHandler.end(false, "command not found.");
                return;
            }
            logger.debug("开始执行命令：{}, {}", type, request.getCommandLine());
            switch (type) {
                case USER_PUBLIC:
                    consumer(command);
                    break;
                case INTERNAL:
                    command.run(processHandler);
                    break;
                default:
                    //ignore
                    break;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private void consumer(Command command) {
        Command current = EnvironmentContext.getCurrentCommand();
        if (null != current && current.isRunning()) {
            String msg = String.format("当前正在执行%s命令，请等待执行完成，或取消、终止当前命令",
                    current.getName());
            logger.warn(msg);
            return;
        }
        ProcessHandler handler = new ProcessHandlerImpl(command.getName());
        EnvironmentContext.setCurrentCommand(command);
        command.run(handler);
    }
}
