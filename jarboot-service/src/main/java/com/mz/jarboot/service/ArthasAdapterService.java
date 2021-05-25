package com.mz.jarboot.service;

/**
 * arthas适配服务
 * @author majianzheng
 */
public interface ArthasAdapterService {
    /**
     * 检查是否安装了arthas
     * @return 是否安装
     */
    boolean checkArthasInstalled();

    /**
     * 启动Arthas调试器
     * @param server 服务名
     */
    void attachToServer(String server);

    /**
     * 获取当前正在使用arthas调试的服务名
     * @return 服务名
     */
    String getCurrentRunning();

    /**
     * 停止当前正在调试的Arthas实例
     */
    void stopCurrentArthasInstance();
}
