package io.github.majianzheng.jarboot.service;

import java.io.InputStream;

/**
 * 升级服务
 * @author majianzheng
 */
public interface UpgradeService {
    /**
     * 升级
     * @param url 升级文件URL
     */
    void upgrade(String url);

    /**
     * 升级
     * @param file 文件名
     * @param is 文件流
     */
    void upgrade(String file, InputStream is);

    /**
     * 更新进度
     * @param msg 消息
     * @param action 动作
     */
    void updateProgress(String msg, int action);
}
