package io.github.majianzheng.jarboot.core.cmd;

import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.api.cmd.spi.CommandProcessor;
import io.github.majianzheng.jarboot.common.protocol.CommandConst;
import io.github.majianzheng.jarboot.common.protocol.CommandRequest;
import io.github.majianzheng.jarboot.common.utils.CommandCliParser;
import io.github.majianzheng.jarboot.core.basic.AgentServiceOperator;
import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.impl.*;
import io.github.majianzheng.jarboot.core.cmd.internal.*;
import io.github.majianzheng.jarboot.core.session.AbstractCommandSession;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create the command instance by the command line
 * @author majianzheng
 */
@SuppressWarnings({"squid:S2386", "squid:S1181", "SpellCheckingInspection"})
public class CommandBuilder {
    private static final Logger logger = LogUtils.getLogger();
    private static final Map<String, Class<? extends AbstractCommand>> CMDS = new ConcurrentHashMap<>(32);
    public static final Map<String, CommandProcessor> EXTEND_MAP = new ConcurrentHashMap<>(16);

    static {
        CMDS.put("bytes", BytesCommand.class);
        CMDS.put("jvm", JvmCommand.class);
        CMDS.put("stdout", StdOutCommand.class);
        CMDS.put("sysprop", SysPropCommand.class);
        CMDS.put("dump", DumpClassCommand.class);
        CMDS.put("heapdump", HeapDumpCommand.class);
        CMDS.put("sysenv", SystemEnvCommand.class);
        CMDS.put("help", HelpCommand.class);

        CMDS.put("jad", JadCommand.class);
        CMDS.put("classloader", ClassLoaderCommand.class);
        CMDS.put("sc", SearchClassCommand.class);
        CMDS.put("sm", SearchMethodCommand.class);
        CMDS.put("ognl", OgnlCommand.class);

        //资源监控类
        CMDS.put("dashboard", DashboardCommand.class);
        CMDS.put("thread", ThreadCommand.class);
        CMDS.put("watch", WatchCommand.class);
        CMDS.put("trace", TraceCommand.class);
        CMDS.put("tt", TimeTunnelCommand.class);
        CMDS.put("stack", StackCommand.class);
        //初始化内部命令实现
        CMDS.put(CommandConst.EXIT_CMD, ExitCommand.class);
        CMDS.put(CommandConst.CANCEL_CMD, CancelCommand.class);
        CMDS.put(CommandConst.HEARTBEAT, HeartbeatCommand.class);
        CMDS.put(CommandConst.INVALID_SESSION_CMD, SessionInvalidCommand.class);
        CMDS.put(CommandConst.SHUTDOWN, ShutdownCommand.class);
        CMDS.put("close", ShutdownCommand.class);
        CMDS.put("window", WindowActiveCommand.class);
        //初始化jdk的spi
        initJdkSpi();
    }

    private CommandBuilder(){}

    public static AbstractCommand build(CommandRequest request, AbstractCommandSession session) {
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
        Class<? extends AbstractCommand> cls = CMDS.getOrDefault(name, null);
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
            String errorMsg = e.getMessage();
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

    public static Map<String, String> getAllCommandDescription() {
        Map<String, String> sortedMap = new LinkedHashMap<>(16);
        CMDS
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
        Class<?> cls = CMDS.getOrDefault(name, null);
        if (null == cls) {
            CommandProcessor p = EXTEND_MAP.getOrDefault(name, null);
            if (null != p) {
                cls = p.getClass();
            }
        }
        return cls;
    }

    private static ExtendCommand createFromSpi(String cmd, String args, AbstractCommandSession session) {
        String errorMsg = String.format("command(%s %s) not found.", cmd, args);
        CommandProcessor processor = EXTEND_MAP.computeIfAbsent(cmd, CommandBuilder::findJdkCmdSpi);
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
            extendCmd.setArgs(parser.getCliArgs());
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
        for (CommandProcessor p : services) {
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
