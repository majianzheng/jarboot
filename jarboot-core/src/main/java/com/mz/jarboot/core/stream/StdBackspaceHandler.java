package com.mz.jarboot.core.stream;

/**
 * stdout 退格处理接口
 * @author majianzheng
 */
public interface StdBackspaceHandler {
    /**
     * 处理控制台退格
     * @param num 退格次数
     */
    void handle(int num);
}
