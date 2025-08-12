package io.github.majianzheng.jarboot.monitor;

import io.github.majianzheng.jarboot.monitor.vo.Server;
import org.springframework.stereotype.Service;

/**
 * 监控服务
 * @author majianzheng
 */
@Service
public class MonitorService {
    public Server getServerInfo() {
        Server server = new Server();
        server.setValueTo();
        return server;
    }
}
