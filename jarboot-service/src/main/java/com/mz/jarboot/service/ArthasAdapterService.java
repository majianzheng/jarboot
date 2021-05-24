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

    
}
