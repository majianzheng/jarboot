package com.mz.jarboot.core.advisor;

/**
 * @author jianzhengma
 */
public interface ClassBytesCallback {
    /**
     * 处理
     * @param className 类名
     * @param classfileBuffer 字节码
     */
    void handler(String className, byte[] classfileBuffer);
}
