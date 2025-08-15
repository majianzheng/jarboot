package io.github.majianzheng.jarboot.service;

import java.io.File;

/**
 * @author majianzheng
 */
public interface UpgradeStoreFileCallback {
    /**
     * 文件存储完成后的回调
     * @param file 文件
     */
    void run(File file);
}
