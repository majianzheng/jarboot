package io.github.majianzheng.jarboot.tools.client;

import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.utils.CommandCliParser;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.tools.client.command.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令构建器
 * @author majianzheng
 */
public class ClientCommandBuilder {
    private static final Map<String, Class<? extends AbstractClientCommand>> CMDS = new ConcurrentHashMap<>(32);
    static {
        CMDS.put("list", ListCommand.class);
        CMDS.put("service", ServiceCommand.class);
        CMDS.put("help", ClientHelpCommand.class);
        CMDS.put("info", InfoCommand.class);
        CMDS.put("deploy", DeployCommand.class);
    }
    private ClientCommandBuilder() {}

    public static AbstractClientCommand build(String commandLine, JarbootClientCli cli) {
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
        AbstractClientCommand command = null;
        Class<? extends AbstractClientCommand> cls = CMDS.getOrDefault(name, null);
        if (null == cls) {
            InnerConsole.getInstance().exec(commandLine);
            return null;
        }

        try {
            command = cls.getConstructor().newInstance();
            command.setClient(cli.client);
            command.setTerminal(cli.terminal);
            command.setClientProxy(cli.proxy);
            command.setLoginHost(cli.host);
            command.setLineReader(cli.lineReader);
            command.setClusterMode(StringUtils.isNotEmpty(cli.runtimeInfo.getHost()));
            command.setRuntimeInfo(cli.runtimeInfo);
            //设置命令名
            command.setName(name);
            //处理命令参数
            CommandCliParser parser = new CommandCliParser(args, command);
            parser.postConstruct();
            return command;
        } catch (Exception e) {
            AnsiLog.error(e);
            return null;
        }
    }

    public static Class<?> getCommandDefineClass(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return CMDS.getOrDefault(name, null);
    }
    public static Map<String, String> getAllCommandDescription() {
        Map<String, String> sortedMap = new LinkedHashMap<>(16);
        CMDS
                .entrySet()
                .stream()
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
        return sortedMap;
    }
}
