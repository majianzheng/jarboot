package io.github.majianzheng.jarboot.client;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Option;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.TaskLifecycle;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.client.command.CommandExecutorFactory;
import io.github.majianzheng.jarboot.client.command.CommandExecutorService;
import io.github.majianzheng.jarboot.client.command.CommandResult;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.utils.BannerUtils;
import io.github.majianzheng.jarboot.common.utils.CommandCliParser;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 客户端命令行工具
 * @author jianzhengma
 */
public class JarbootClientCli {
    private String host;
    private String username;
    private String password;

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

    public static void main(String[] args) {
        BannerUtils.print();
        AnsiLog.info("Jarboot client cli>>>");
        JarbootClientCli clientCli = new JarbootClientCli();
        CommandCliParser commandCliParser = new CommandCliParser(args, clientCli);
        commandCliParser.postConstruct();
        if (StringUtils.isEmpty(clientCli.host)) {
            clientCli.host = System.getenv(CommonConst.JARBOOT_HOST_ENV);
            if (StringUtils.isEmpty(clientCli.host)) {
                clientCli.host = "127.0.0.1:9899";
            }
        }
        //登录
        clientCli.login();
        //开始执行
        clientCli.run();
    }

    private void login() {
        AnsiLog.info("Login to Jarboot server: {}", this.host);
        if (StringUtils.isEmpty(username) && null != System.console()) {
            username = System.console().readLine("username:");
        }
        if (StringUtils.isEmpty(password) && null != System.console()) {
            password = new String(System.console().readPassword("password:"));
        }
        //登录认证
        String version = ClientProxy
                .Factory
                .createClientProxy(host, username, password)
                .getVersion();
        AnsiLog.info("Login success, jarboot server version: {}", version);
    }

    protected void run() {
        //test
        final String demo = "demo-server";
        ServiceManager client = new ServiceManagerClient(this.host, null, null);
        List<ServiceInstance> list = client.getServiceList();
        AnsiLog.info("list:{}", list);

        AnsiLog.info("jvm list: {}", client.getJvmProcesses());
        client.registerSubscriber(demo, TaskLifecycle.PRE_START, new Subscriber<TaskLifecycleEvent>() {
            @Override
            public void onEvent(TaskLifecycleEvent event) {
                AnsiLog.info("event received:{}", event);
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return TaskLifecycleEvent.class;
            }
        });
        client.registerSubscriber(demo, TaskLifecycle.EXCEPTION_OFFLINE, new Subscriber<TaskLifecycleEvent>() {
            @Override
            public void onEvent(TaskLifecycleEvent event) {
                AnsiLog.info("exception offline:{}", event);
            }

            @Override
            public Class<? extends JarbootEvent> subscribeType() {
                return TaskLifecycleEvent.class;
            }
        });

        SettingService setting = new SettingClient(this.host, null, null);
        AnsiLog.info("system setting: {}", setting.getSystemSetting());


        //测试命令执行
        CommandExecutorService executor = CommandExecutorFactory
                .createCommandExecutor(demo, host, username, password);
        try {
            //执行命令
            Future<CommandResult> future = executor
                    .execute("pwd", event -> AnsiLog.info("command notify:{}", event));
            //使用Future同步获取结果
            AnsiLog.info("result: {}", future.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        }
        //测试取消执行
        try {
            //执行命令
            Future<CommandResult> future = executor
                    .execute("dashboard", event -> AnsiLog.info("command notify:{}", event));
            //等待几秒钟
            Thread.sleep(6000);
            //取消执行
            AnsiLog.info("cancel: {}, result:{}", future.cancel(false), future.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        }

        try {
            System.in.read();
        } catch (IOException e) {
            AnsiLog.error(e);
        }
    }
}
