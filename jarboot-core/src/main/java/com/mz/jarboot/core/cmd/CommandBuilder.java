package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.CommandConst;
import com.mz.jarboot.common.CommandRequest;
import com.mz.jarboot.common.CommandType;
import com.mz.jarboot.core.cmd.impl.*;
import com.mz.jarboot.core.cmd.internal.CancelCommandImpl;
import com.mz.jarboot.core.cmd.internal.ExitCommandImpl;
import com.mz.jarboot.core.cmd.internal.SessionInvalidCommandImpl;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create the command instance by the command line
 * @author jianzhengma
 */
public class CommandBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static final Map<String, Class<? extends Command>> commandMap = new ConcurrentHashMap<>();
    private static final Map<String, Class<? extends Command>> internalCommandMap = new ConcurrentHashMap<>();
    static {
        commandMap.put("bytes", BytesCommandImpl.class);
        commandMap.put("jvm", JvmCommandImpl.class);
        commandMap.put("trace", TraceCommandImpl.class);
        commandMap.put("sysprop", SysPropCommandImpl.class);
        //初始化内部命令实现
        internalCommandMap.put(CommandConst.EXIT_CMD, ExitCommandImpl.class);
        internalCommandMap.put(CommandConst.CANCEL_CMD, CancelCommandImpl.class);
        internalCommandMap.put(CommandConst.INVALID_SESSION_CMD, SessionInvalidCommandImpl.class);
    }
    private CommandBuilder(){}
    public static Command build(CommandRequest request) {
        CommandType type = request.getCommandType();
        String commandLine = request.getCommandLine();
        int p = commandLine.indexOf(' ');
        String name;
        String args;
        if (-1 == p) {
            name = commandLine;
            args = "";
        } else {
            name = commandLine.substring(0, p);
            args = commandLine.substring(p + 1);
        }
        name = name.toLowerCase();
        logger.info("type:{}, cmd:{}, args:{}", type, name, args);
        Class<? extends Command> cls = (CommandType.INTERNAL.equals(type)) ?
                internalCommandMap.getOrDefault(name, null) :
                commandMap.getOrDefault(name, null);
        if (null == cls) {
            logger.info("can not find class. {}", name);
            return null;
        }
        try {
            //构建command实例
            Constructor<? extends Command> constructor = cls.getConstructor();
            Command command = constructor.newInstance();

            //填充成员变量
            command.setName(name);
            if (!args.isEmpty()) {
                command.setArgs(args);
            }
            String sessionId = request.getSessionId();
            if (!StringUtils.isEmpty(sessionId)) {
                command.setSessionId(sessionId);
            }
            return command;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return null;
    }
}
