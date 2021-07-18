package com.mz.jarboot.core.advisor;

public interface ClassBytesCallback {
    void handler(String className, byte[] classfileBuffer);
}
