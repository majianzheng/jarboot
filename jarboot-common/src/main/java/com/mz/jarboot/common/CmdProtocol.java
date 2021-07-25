package com.mz.jarboot.common;

/**
 * 命令交互协议
 * @author majianzheng
 */
public interface CmdProtocol {

    /**
     * 序列化为字符串用于传输
     * @return 字符串
     */
    String toRaw();

    /**
     * 反序列化为对象
     * @param raw 字符串
     */
    void fromRaw(String raw);
}
