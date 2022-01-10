package com.mz.jarboot.client;

import com.mz.jarboot.api.JarbootFactory;
import com.mz.jarboot.api.pojo.ServerRunning;
import com.mz.jarboot.api.service.ServerMgrService;
import com.mz.jarboot.common.AnsiLog;

import java.util.List;

/**
 * 客户端命令行工具
 * @author jianzhengma
 */
public class JarbootClientCli {
    public static void main(String[] args) {
        AnsiLog.info("Jarboot client cli");
        ServerMgrService client = JarbootFactory
                .createServerManager("localhost:9899", "jarboot", "jarboot");
        List<ServerRunning> list = client.getServerList();
        AnsiLog.info("list:{}", list);
    }
}
