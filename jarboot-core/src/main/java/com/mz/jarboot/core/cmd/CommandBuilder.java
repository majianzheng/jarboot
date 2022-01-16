package com.mz.jarboot.core.cmd;

import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import com.mz.jarboot.common.protocol.CommandConst;
import com.mz.jarboot.common.protocol.CommandRequest;
import com.mz.jarboot.common.protocol.CommandType;
import com.mz.jarboot.common.utils.CommandCliParser;
import com.mz.jarboot.core.basic.AgentServiceOperator;
import com.mz.jarboot.core.basic.EnvironmentContext;
import com.mz.jarboot.core.cmd.impl.*;
import com.mz.jarboot.core.cmd.internal.*;
import com.mz.jarboot.core.session.AbstractCommandSession;
import com.mz.jarboot.core.utils.LogUtils;
import com.mz.jarboot.common.utils.StringUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create the command instance by the command line
 * @author majianzheng
 */
@SuppressWarnings("all")
public class CommandBuilder {
    private static final Logger logger = LogUtils.getLogger();
    private static final Map<String, Class<? extends AbstractCommand>> commandMap = new ConcurrentHashMap<>(32);
    public static final Map<String, CommandProcessor> EXTEND_MAP = new ConcurrentHashMap<>(16);

    static {
        commandMap.put("bytes", BytesCommand.class);
        commandMap.put("jvm", JvmCommand.class);
        commandMap.put("stdout", StdOutCommand.class);
        commandMap.put("sysprop", SysPropCommand.class);
        commandMap.put("dump", DumpClassCommand.class);
        commandMap.put("heapdump", HeapDumpCommand.class);
        commandMap.put("sysenv", SystemEnvCommand.class);
        commandMap.put("help", HelpCommand.class);

        commandMap.put("jad", JadCommand.class);
        commandMap.put("classloader", ClassLoaderCommand.class);
        commandMap.put("sc", SearchClassCommand.class);
        commandMap.put("sm", SearchMethodCommand.class);
        commandMap.put("ognl", OgnlCommand.class);

        //资源监控类
        commandMap.put("dashboard", DashboardCommand.class);
        commandMap.put("thread", ThreadCommand.class);
        commandMap.put("watch", WatchCommand.class);
        commandMap.put("trace", TraceCommand.class);
        commandMap.put("tt", TimeTunnelCommand.class);
        commandMap.put("stack", StackCommand.class);
        //初始化内部命令实现
        commandMap.put(CommandConst.EXIT_CMD, ExitCommand.class);
        commandMap.put(CommandConst.CANCEL_CMD, CancelCommand.class);
        commandMap.put(CommandConst.HEARTBEAT, HeartbeatCommand.class);
        commandMap.put(CommandConst.INVALID_SESSION_CMD, SessionInvalidCommand.class);
        commandMap.put(CommandConst.SHUTDOWN, ShutdownCommand.class);
        commandMap.put("close", ShutdownCommand.class);
        //初始化jdk的spi
        initJdkSpi();
    }

    private CommandBuilder(){}

    public static AbstractCommand build(CommandRequest request, AbstractCommandSession session) {
        CommandType type = request.getCommandType();
        String commandLine = request.getCommandLine();
        int p = commandLine.indexOf(' ');
        String name;
        String args;
        if (-1 == p) {
            name = commandLine;
            args = StringUtils.EMPTY;
        } else {
            name = commandLine.substring(0, p);
            args = commandLine.substring(p + 1);
        }
        name = name.toLowerCase();
        AbstractCommand command = null;
        Class<? extends AbstractCommand> cls = commandMap.getOrDefault(name, null);
        String errorMsg = StringUtils.EMPTY;
        if (null == cls) {
            // 尝试从SPI加载扩展命令
            return createFromSpi(name, args, session);
        }
        try {
            command = cls.getConstructor().newInstance();
            command.setSession(session);
            //设置命令名
            command.setName(name);
            //处理命令参数
            CommandCliParser parser = new CommandCliParser(args, command);
            parser.postConstruct();
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            logger.error(errorMsg, e);
            if (null != command) {
                command.printHelp();
            }
            if (null == errorMsg) {
                session.end();
            } else {
                session.end(false, errorMsg);
                AgentServiceOperator.noticeWarn(errorMsg, session.getSessionId());
            }
            command = null;
        }
        return command;
    }

    public static Map<String, String> getAllCommandDesciption() {
        Map<String, String> sortedMap = new LinkedHashMap<>(16);
        commandMap
                .entrySet()
                .stream()
                .filter(v -> !v.getValue().getSuperclass().equals(AbstractInternalCommand.class))
                .sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
                .forEachOrdered(v -> {
                    Class<?> cls = v.getValue();
                    Summary summary = cls.getAnnotation(Summary.class);
                    String desc = StringUtils.EMPTY;
                    if (null != summary) {
                        desc = summary.value();
                    }
                    sortedMap.put(v.getKey(), desc);
                });
        EXTEND_MAP
                .entrySet()
                .stream()
                .sorted((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()))
                .forEach(v -> {
                    Class<?> cls = v.getValue().getClass();
                    Summary summary = cls.getAnnotation(Summary.class);
                    String desc = StringUtils.EMPTY;
                    if (null != summary) {
                        desc = summary.value();
                    }
                    sortedMap.put(v.getKey(), desc);
                });
        return sortedMap;
    }

    public static Class<?> getCommandDefineClass(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        Class<?> cls = commandMap.getOrDefault(name, null);
        if (null == cls) {
            CommandProcessor p = EXTEND_MAP.getOrDefault(name, null);
            if (null != p) {
                cls = p.getClass();
            }
        }
        return cls;
    }

    private static ExtendCommand createFromSpi(String cmd, String args, AbstractCommandSession session) {
        String errorMsg = "command not found.";
        CommandProcessor processor = EXTEND_MAP.computeIfAbsent(cmd, k -> findJdkCmdSpi(k));
        if (null == processor) {
            AgentServiceOperator.noticeInfo(errorMsg, session.getSessionId());
            session.end(false, errorMsg);
            return null;
        }

        ExtendCommand extendCmd = null;
        try {
            //若非单例使用原型构建新实例，防止多会话冲突
            processor = processor.isSingleton() ? processor : processor.getClass().getConstructor().newInstance();
            //使用新构建的processor构建扩展类命令
            extendCmd = new ExtendCommand(processor);
            extendCmd.setName(cmd);
            extendCmd.setSession(session);
            CommandCliParser parser = new CommandCliParser(args, processor);
            parser.postConstruct();
            extendCmd.setArgs(parser.getSplitedArgs());
            processor.postConstruct(
                    EnvironmentContext.getInstrumentation(),
                    EnvironmentContext.getAgentClient().getServiceName());
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            errorMsg = e.getMessage();
            if (null == extendCmd) {
                AgentServiceOperator.noticeError(errorMsg, session.getSessionId());
            } else {
                extendCmd.printHelp();
                extendCmd = null;
            }
            if (null == errorMsg) {
                session.end();
            } else {
                session.end(false, errorMsg);
            }
        }
        return extendCmd;
    }

    private static CommandProcessor findJdkCmdSpi(String cmd) {
        return EXTEND_MAP.getOrDefault(cmd, null);
    }

    private static void initJdkSpi() {
        ServiceLoader<CommandProcessor> services = ServiceLoader.load(CommandProcessor.class);
        Iterator<CommandProcessor> iter = services.iterator();
        while (iter.hasNext()) {
            CommandProcessor p = iter.next();
            try {
                Name name = p.getClass().getAnnotation(Name.class);
                if (null != name && null != EXTEND_MAP.putIfAbsent(name.value(), p)) {
                    logger.warn("User-defined command {} is repetitive in jdk SPI.", name.value());
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
