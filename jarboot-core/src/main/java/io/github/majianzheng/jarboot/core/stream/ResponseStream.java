package io.github.majianzheng.jarboot.core.stream;


/**
 * handle the command's response to jarboot-server
 * @author majianzheng
 */
public interface ResponseStream {
    /**
     * 写响应数据
     * @param data 数据
     */
    void write(byte[] data);
}
