package com.mz.jarboot.core.cmd;

import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import com.mz.jarboot.common.*;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.impl.*;
import com.mz.jarboot.core.cmd.internal.CancelCommand;
import com.mz.jarboot.core.cmd.internal.ExitCommand;
import com.mz.jarboot.core.cmd.internal.SessionInvalidCommand;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandCoreSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create the command instance by the command line
 * @author majianzheng
 */
@SuppressWarnings("all")
public class CommandBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static final Map<String, Class<? extends AbstractCommand>> commandMap = new ConcurrentHashMap<>(32);
    private static final Map<String, CommandProcessor> extendMap = new ConcurrentHashMap<>(16);
    static {
        commandMap.put("bytes", BytesCommand.class);
        commandMap.put("jvm", JvmCommand.class);
        commandMap.put("stdout", StdOutCommand.class);
        commandMap.put("sysprop", SysPropCommand.class);
        commandMap.put("heapdump", HeapDumpCommand.class);

        commandMap.put("jad", JadCommand.class);
        commandMap.put("classloader", ClassLoaderCommand.class);
        commandMap.put("sc", SearchClassCommand.class);

        //资源监控类
        commandMap.put("dashboard", DashboardCommand.class);
        commandMap.put("thread", ThreadCommand.class);
        commandMap.put("watch", WatchCommand.class);
        commandMap.put("trace", TraceCommand.class);
        //初始化内部命令实现
        commandMap.put(CommandConst.EXIT_CMD, ExitCommand.class);
        commandMap.put(CommandConst.CANCEL_CMD, CancelCommand.class);
        commandMap.put(CommandConst.INVALID_SESSION_CMD, SessionInvalidCommand.class);
    }
    private CommandBuilder(){}
    public static AbstractCommand build(CommandRequest request, CommandCoreSession session) {
        CommandType type = request.getCommandType();
        String commandLine = request.getCommandLine();
        int p = commandLine.indexOf(' ');
        String name;
        String args;
        if (-1 == p) {
            name = commandLine;
            args = CoreConstant.EMPTY_STRING;
        } else {
            name = commandLine.substring(0, p);
            args = commandLine.substring(p + 1);
        }
        name = name.toLowerCase();
        AbstractCommand command = null;
        Class<? extends AbstractCommand> cls = commandMap.getOrDefault(name, null);
        if (null == cls) {
            command = createFromSpi(name, args, session);
            if (null != command) {
                return command;
            }
            logger.info("can not find class. {}, type:{}, args:{}", name, type, args);
            session.end(false, "command not found.");
            return null;
        }
        try {
            command = cls.getConstructor().newInstance();
            //处理命令参数
            CommandArgsParser parser = new CommandArgsParser(args, command);
            parser.postConstruct();
            command.setSession(session);
            //设置命令名
            command.setName(name);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            session.end(false, e.getMessage());
        }
        return command;
    }

    private static AbstractCommand createFromSpi(String cmd, String args, CommandCoreSession session) {
        CommandProcessor processor = extendMap.computeIfAbsent(cmd, k -> {
            return findJdkCmdSpi(cmd);
        });
        if (null == processor) {
            return null;
        }

        CommandArgsParser parser = new CommandArgsParser(args, processor);
        parser.postConstruct();
        ExtendCommand extendCmd = new ExtendCommand(processor, parser.getSplitedArgs());
        extendCmd.setSession(session);
        processor.postConstruct(EnvironmentContext.getInstrumentation(), EnvironmentContext.getServer());
        return extendCmd;
    }

    private static CommandProcessor findJdkCmdSpi(String cmd) {
        ServiceLoader<CommandProcessor> services = ServiceLoader.load(CommandProcessor.class);
        Iterator<CommandProcessor> iter = services.iterator();
        while (iter.hasNext()) {
            CommandProcessor p = iter.next();
            try {
                Name name = p.getClass().getAnnotation(Name.class);
                if (null != name && cmd.equals(name.value())) {
                    return p;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    private static void printSummary(AbstractCommand command) {
        Class<? extends AbstractCommand> cls = command.getClass();
        Summary summary = cls.getAnnotation(Summary.class);
        Description usage = cls.getAnnotation(Description.class);
        StringBuilder sb = new StringBuilder();
        if (null != summary) {
            sb.append(summary.value()).append(CoreConstant.BR);
        }
        if (null != usage) {
            sb.append(usage.value()).append(CoreConstant.BR);
        }
        command.getSession().console(sb.toString());
    }
}
