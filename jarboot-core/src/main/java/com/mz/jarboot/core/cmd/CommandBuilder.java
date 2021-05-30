package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.CommandType;
import com.mz.jarboot.core.cmd.impl.ExitCommandImpl;
import com.mz.jarboot.core.cmd.impl.JvmCommandImpl;
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
        internalCommandMap.put("exit", ExitCommandImpl.class);
    }
    private CommandBuilder(){}
    public static Command build(CommandType type, String commandLine) {
        int p = commandLine.indexOf(' ');
        String cmd;
        String args;
        if (-1 == p) {
            cmd = commandLine;
            args = "";
        } else {
            cmd = commandLine.substring(0, p);
            args = commandLine.substring(p + 1);
        }
        cmd = cmd.toLowerCase();
        logger.info("type:{}, cmd:{}, args:{}", type, cmd, args);
        Class<? extends Command> cls = (CommandType.INTERNAL.equals(type)) ?
                internalCommandMap.getOrDefault(cmd, null) :
                commandMap.getOrDefault(cmd, null);
        if (null == cls) {
            logger.info("can not find class. {}", cmd);
            return null;
        }
        try {
            Constructor<? extends Command> constructor = cls.getConstructor(String.class, String.class);
            return constructor.newInstance(cmd, args);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return null;
    }
}
