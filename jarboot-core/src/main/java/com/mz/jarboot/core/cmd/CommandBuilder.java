package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.CommandType;
import com.mz.jarboot.core.cmd.impl.CancelCommandImpl;
import com.mz.jarboot.core.cmd.impl.ExitCommandImpl;
import com.mz.jarboot.core.cmd.impl.JvmCommandImpl;
import com.mz.jarboot.core.cmd.impl.TraceCommandImpl;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Create the command instance by the command line
 * @author jianzhengma
 */
public class CommandBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static Map<String, Class<? extends Command>> commandMap = new HashMap<>();
    private static Map<String, Class<? extends Command>> internalCommandMap = new HashMap<>();
    static {
        commandMap.put("jvm", JvmCommandImpl.class);
        commandMap.put("trace", TraceCommandImpl.class);
        //初始化内部命令实现
        internalCommandMap.put("exit", ExitCommandImpl.class);
        internalCommandMap.put("cancel", CancelCommandImpl.class);
    }
    private CommandBuilder(){}
    public static Command build(CommandType type, String commandLine) {
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
            Constructor<? extends Command> constructor = cls.getConstructor();
            Command command = constructor.newInstance();
            command.setName(name);
            if (null != args && !args.isEmpty()) {
                command.setArgs(args);
            }
            return command;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return null;
    }
}
