package com.mz.jarboot.api;

/**
 * Agent Service<br>
 * Use {@link JarbootFactory} to create
 * @author majianzheng
 */
public interface AgentService {
    /**
     * 服务启动完成，通知Jarboot Server已完成<br>
     * 当没有主动调用该方法时，默认情况下是等待控制一段时间没有输出时判定为启动完成<br>
     * 判定时间可通过VM参数：-Dstart.determine.time=5000 来指定，默认5000毫秒
     */
    void setStarted();

    /**
     * 获取服务名
     * @return 服务名
     */
    String getServerName();
}
