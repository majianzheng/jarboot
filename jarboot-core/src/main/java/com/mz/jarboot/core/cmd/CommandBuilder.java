package com.mz.jarboot.core.cmd;

import com.mz.jarboot.common.*;
import com.mz.jarboot.core.cmd.annotation.*;
import com.mz.jarboot.core.cmd.impl.*;
import com.mz.jarboot.core.cmd.internal.CancelCommandImpl;
import com.mz.jarboot.core.cmd.internal.ExitCommandImpl;
import com.mz.jarboot.core.cmd.internal.SessionInvalidCommandImpl;
import com.mz.jarboot.core.constant.CoreConstant;
import com.mz.jarboot.core.session.CommandSession;
import com.mz.jarboot.core.utils.BasicTypeConvert;
import com.mz.jarboot.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create the command instance by the command line
 * @author jianzhengma
 */
@SuppressWarnings("all")
public class CommandBuilder {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private static final Map<String, Class<? extends Command>> commandMap = new ConcurrentHashMap<>();
    private static final Map<String, Class<? extends Command>> internalCommandMap = new ConcurrentHashMap<>();
    static {
        commandMap.put("bytes", BytesCommand.class);
        commandMap.put("jvm", JvmCommand.class);
        commandMap.put("sysprop", SysPropCommandImpl.class);

        commandMap.put("jad", JadCommand.class);
        commandMap.put("classloader", ClassLoaderCommand.class);

        //资源监控类
        commandMap.put("dashboard", DashboardCommand.class);
        commandMap.put("thread", ThreadCommand.class);
        commandMap.put("watch", WatchCommand.class);
        commandMap.put("trace", TraceCommandImpl.class);
        //初始化内部命令实现
        internalCommandMap.put(CommandConst.EXIT_CMD, ExitCommandImpl.class);
        internalCommandMap.put(CommandConst.CANCEL_CMD, CancelCommandImpl.class);
        internalCommandMap.put(CommandConst.INVALID_SESSION_CMD, SessionInvalidCommandImpl.class);
    }
    private CommandBuilder(){}
    public static Command build(CommandRequest request, CommandSession session) {
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
        //处理命令参数
        CommandArgsParser parser = new CommandArgsParser(args);
        try {
            //构建command实例
            Constructor<? extends Command> constructor = cls.getConstructor();
            Command command = constructor.newInstance();
            command.setSession(session);

            //根据注解，填充命令参数
            Method[] methods = cls.getMethods();
            for (Method method : methods) {
                setField(method, command, parser);
            }

            //设置命令名
            command.setName(name);
            return command;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            session.console(e.getMessage());
            session.end(false);
        }
        return null;
    }

    private static void setField(Method method, Command command, CommandArgsParser parser) {
        if (!method.getName().startsWith("set")) {
            return;
        }
        Description description = method.getAnnotation(Description.class);
        DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new MzException("命令定义错误，set方法应该存在一个参数！" + paramTypes.length);
        }
        fillArgument(method, command, parser, description, defaultValue, paramTypes[0]);
        fillOptions(method, command, parser, description, paramTypes[0]);
    }

    private static void fillArgument(Method method, Command command, CommandArgsParser parser,
                                     Description description, DefaultValue defaultValue, Class<?> paramType) {
        Argument argument = method.getAnnotation(Argument.class);
        if (null == argument) {
            return;
        }
        int index = argument.index();
        //获取
        String arg = parser.getArgument(index);
        if (StringUtils.isEmpty(arg) && null != defaultValue) {
            arg = defaultValue.value();
        }
        if (StringUtils.isEmpty(arg) && argument.required()) {
            //提示错误
            throw new MzException(ResultCodeConst.INVALID_PARAM,
                    formatParamError(argument.argName(), description.value()));
        }
        if (!StringUtils.isEmpty(arg)) {
            Object val = BasicTypeConvert.convert(arg, paramType);
            callSetMethod(method, command, val);
        }
    }

    private static void fillOptions(Method method, Command command, CommandArgsParser parser,
                                    Description description, Class<?> paramType) {
        Option option = method.getAnnotation(Option.class);
        if (null == option) {
            return;
        }
        //option
        String shortName = option.shortName();
        String longName = option.longName();
        if (option.acceptMultipleValues()) {
            List<String> values = parser.getMultiOptionValue(shortName, longName);
            callSetMethod(method, command, values);
            return;
        }
        if (option.flag()) {
            callSetMethod(method, command, parser.hasOption(shortName, longName));
            return;
        }
        String optVal = parser.getOptionValue(shortName, longName);
        Object arg = BasicTypeConvert.convert(optVal, paramType);
        if (null != arg) {
            callSetMethod(method, command, arg);
            return;
        }
        if (option.required()) {
            //提示错误
            throw new MzException(ResultCodeConst.INVALID_PARAM,
                    formatParamError(option.argName(), description.value()));
        }
    }

    private static void callSetMethod(Method m, Command command, Object arg) {
        try {
            m.invoke(command, arg);
        } catch (Exception e) {
            throw new MzException(e.getMessage(), e);
        }
    }
    private static String formatParamError(String name, String desc) {
        return String.format("The argument '%s' is required, description: %s", name, desc);
    }
    private static void printSummary(Command command) {
        Class<? extends Command> cls = command.getClass();
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
