package io.github.majianzheng.jarboot.tools.client;

import io.github.majianzheng.jarboot.api.cmd.annotation.Argument;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Option;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.client.ClientProxy;
import io.github.majianzheng.jarboot.client.ClusterOperator;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.CacheDirHelper;
import io.github.majianzheng.jarboot.common.utils.BannerUtils;
import io.github.majianzheng.jarboot.common.utils.CommandCliParser;
import io.github.majianzheng.jarboot.common.utils.OSUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.tools.client.command.AbstractClientCommand;
import io.github.majianzheng.jarboot.tools.common.Utils;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 客户端命令行工具
 * @author jianzhengma
 */
@SuppressWarnings({"java:S106", "java:S135"})
public class JarbootClientCli implements Subscriber<TaskLifecycleEvent> {
    private static final String TOKEN_ENV = "JARBOOT_TOKEN";
    private static final String USER_ENV = "JARBOOT_USER";
    private static final String PWD_ENV = "JARBOOT_PWD";
    String host;
    String username;
    String password;
    String token;
    Terminal terminal;
    LineReader lineReader;
    ClientProxy proxy;
    ClusterOperator client;
    ServerRuntimeInfo runtimeInfo;
    private String bash;

    @Option(shortName = "h", longName = "host")
    @Description("The Jarboot host. ig: 127.0.0.1:9899")
    public void setHost(String host) {
        this.host = host;
    }

    @Option(shortName = "u", longName = "user")
    @Description("The Jarboot username")
    public void setUsername(String username) {
        this.username = username;
    }

    @Option(shortName = "p", longName = "password")
    @Description("The Jarboot password")
    public void setPassword(String password) {
        this.password = password;
    }

    @Option(shortName = "token", longName = "token")
    @Description("The Jarboot access token")
    public void setToken(String token) {
        this.token = token;
    }

    @Argument(argName = "bash", index = 0, required = false)
    @Description("bash script.")
    public void setAction(String bash) {
        this.bash = bash;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("jline.WindowsTerminal.input.encoding", "UTF-8");
        System.setProperty("jline.WindowsTerminal.output.encoding", "UTF-8");
        Utils.getJarbootHome();
        CacheDirHelper.clean();
        BannerUtils.print();
        JarbootClientCli clientCli = new JarbootClientCli();
        CommandCliParser commandCliParser = new CommandCliParser(args, clientCli);
        commandCliParser.postConstruct();
        if (StringUtils.isEmpty(clientCli.host)) {
            clientCli.host = System.getenv(CommonConst.JARBOOT_HOST_ENV);
            if (StringUtils.isEmpty(clientCli.host)) {
                clientCli.host = "127.0.0.1:9899";
            }
        }
        if (StringUtils.isEmpty(clientCli.token)) {
            clientCli.token = System.getenv(TOKEN_ENV);
        }
        if (StringUtils.isEmpty(clientCli.username)) {
            clientCli.username = System.getenv(USER_ENV);
        }
        if (StringUtils.isEmpty(clientCli.password)) {
            clientCli.password = System.getenv(PWD_ENV);
        }
        //登录
        clientCli.login();
        //开始执行
        clientCli.run();
    }

    private void login() throws IOException {
        AnsiLog.println("Login to Jarboot server: {}", this.host);
        terminal = TerminalBuilder
                .builder()
                .name("jarboot client terminal")
                .streams(System.in, System.out)
                .system(!OSUtils.isWindows())
                .encoding(StandardCharsets.UTF_8)
                .color(true)
                .build();

        lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .option(LineReader.Option.ERASE_LINE_ON_FINISH, OSUtils.isWindows())
                .build();
        //登录认证
        if (StringUtils.isNotEmpty(token)) {
            try {
                proxy = ClientProxy
                        .Factory
                        .createClientProxy(host, token);
            } catch (Exception e) {
                AnsiLog.error("Login failed, please check your token.");
                System.exit(1);
            }
        } else {
            if (StringUtils.isEmpty(username)) {
                username = lineReader.readLine("username:");
            }
            if (StringUtils.isEmpty(password)) {
                password = lineReader.readLine("password:");
                if (StringUtils.isEmpty(password)) {
                    password = lineReader.readLine("password:");
                }
            }
            if (StringUtils.isEmpty(password)) {
                AnsiLog.error("Password is empty.");
                System.exit(1);
            }
            try {
                proxy = ClientProxy
                        .Factory
                        .createClientProxy(host, username, password);
            } catch (Exception e) {
                AnsiLog.error("Login failed, please check your username or password.");
                System.exit(1);
            }
        }
        runtimeInfo = proxy.getRuntimeInfo();
        AnsiLog.println("Login success, jarboot server version: {}, cluster:{}",
                runtimeInfo.getVersion(), StringUtils.isNotEmpty(runtimeInfo.getHost()));
    }

    protected void run() {
        InnerConsole.getInstance().init(lineReader);

        client = new ClusterOperator(this.host, null, null);
        client.registerSubscriber(this);
        if (StringUtils.isNotEmpty(bash)) {
            execBash();
            return;
        }
        AnsiLog.println("Diagnose command, try running `help`");
        final String prefix = StringUtils.isEmpty(runtimeInfo.getHost()) ? "jarboot$> " : "cluster$> ";
        final Character mask = OSUtils.isWindows() ? (char)0 : null;
        for (;;) {
            String inputLine = lineReader.readLine(prefix, mask);
            if ("q".equals(inputLine) || "quit".equals(inputLine) || "exit".equals(inputLine) || "bye".equals(inputLine)) {
                break;
            }
            if (StringUtils.isEmpty(inputLine)) {
                continue;
            }
            // 打印出用户输入的内容
            AbstractClientCommand command = ClientCommandBuilder.build(inputLine, this);
            if (null != command) {
                try {
                    command.run();
                } catch (Exception e) {
                    AnsiLog.error(e.getMessage(), e);
                }
            }
        }
        client.deregisterSubscriber(this);
    }

    private void execBash() {
        List<String> lines = StringUtils.toLines(bash);
        lines.forEach(inputLine -> {
            String[] commands = StringUtils.tokenizeToStringArray(inputLine, ";");
            for (String command : commands) {
                if (StringUtils.isEmpty(command)) {
                    continue;
                }
                AbstractClientCommand cmd = ClientCommandBuilder.build(command, this);
                if (null != cmd) {
                    try {
                        cmd.run();
                    } catch (Exception e) {
                        AnsiLog.error(e.getMessage(), e);
                    }
                }
            }
        });
        client.deregisterSubscriber(this);
    }

    @Override
    public void onEvent(TaskLifecycleEvent event) {
        String name = StringUtils.isEmpty(event.getSetting().getHost()) ? event.getSetting().getName() : (event.getSetting().getName() + "@" + event.getSetting().getHost());
        AnsiLog.println("{}: {}\n", name, event.getStatus());
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return TaskLifecycleEvent.class;
    }
}
