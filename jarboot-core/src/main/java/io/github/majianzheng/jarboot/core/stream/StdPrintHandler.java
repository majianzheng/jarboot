package io.github.majianzheng.jarboot.core.stream;

/**
 * stdout 处理接口
 * @author majianzheng
 */
public interface StdPrintHandler {
    /**
     * 处理控制消息
     * @param text 消息
     */
    void handle(String text);
}
