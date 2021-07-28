package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.*;
import com.mz.jarboot.core.cmd.annotation.*;
import com.mz.jarboot.core.cmd.impl.*;
import com.mz.jarboot.core.cmd.internal.CancelCommand;
import com.mz.jarboot.core.cmd.internal.ExitCommand;
import com.mz.jarboot.core.cmd.internal.SessionInvalidCommand;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create the command instance by the command line
 * @author majianzheng
 */
@SuppressWarnings("all")
public class CommandBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static final Map<String, Class<? extends AbstractCommand>> commandMap = new ConcurrentHashMap<>();
    private static final Map<String, Class<? extends AbstractCommand>> internalCommandMap = new ConcurrentHashMap<>();
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
        internalCommandMap.put(CommandConst.EXIT_CMD, ExitCommand.class);
        internalCommandMap.put(CommandConst.CANCEL_CMD, CancelCommand.class);
        internalCommandMap.put(CommandConst.INVALID_SESSION_CMD, SessionInvalidCommand.class);
    }
    private CommandBuilder(){}
    public static AbstractCommand build(CommandRequest request, CommandSession session) {
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
        Class<? extends AbstractCommand> cls = (CommandType.INTERNAL.equals(type)) ?
                internalCommandMap.getOrDefault(name, null) :
                commandMap.getOrDefault(name, null);
        if (null == cls) {
            logger.info("can not find class. {}, type:{}, args:{}", name, type, args);
            session.end(false, "command not found.");
            return null;
        }
        try {
            //处理命令参数
            CommandArgsParser parser = new CommandArgsParser(args, cls);

            //构建command实例
            AbstractCommand command = parser.getCommand();
            command.setSession(session);
            //设置命令名
            command.setName(name);
            return command;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            session.end(false, e.getMessage());
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
