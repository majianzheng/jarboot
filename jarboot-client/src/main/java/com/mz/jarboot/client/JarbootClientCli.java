package com.mz.jarboot.client;

import com.mz.jarboot.api.JarbootFactory;
import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Option;
import com.mz.jarboot.api.constant.CommonConst;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.service.ServerMgrService;
import com.mz.jarboot.client.utlis.HttpRequestOperator;
import com.mz.jarboot.common.AnsiLog;
import com.mz.jarboot.common.utils.BannerUtils;
import com.mz.jarboot.common.utils.CommandCliParser;
import com.mz.jarboot.common.utils.StringUtils;

import java.util.List;

/**
 * 客户端命令行工具
 * @author jianzhengma
 */
public class JarbootClientCli {
    private String host;

    @Option(shortName = "h", longName = "host")
    @Description("The Jarboot host. ig: 127.0.0.1:9899")
    public void setHost(String host) {
        this.host = host;
    }

    public static void main(String[] args) {
        if (null == System.console()) {
            AnsiLog.info("Must in console.");
            return;
        }
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
        String username = System.console().readLine("username:");
        String password = new String(System.console().readPassword("password:"));
        //登录认证
        final String api = CommonConst.CLOUD_CONTEXT + "/version";
        String version = ClientProxy
                .Factory
                .createClientProxy(host, username, password)
                .reqApi(api, StringUtils.EMPTY, HttpRequestOperator.HttpMethod.GET);
        AnsiLog.info("Login success, jarboot server version: {}", version);
    }

    private void run() {
        //test
        ServerMgrService client = JarbootFactory
                .createServerManager(this.host, null, null);
        List<ServerRunning> list = client.getServerList();
        AnsiLog.info("list:{}", list);
    }
}
