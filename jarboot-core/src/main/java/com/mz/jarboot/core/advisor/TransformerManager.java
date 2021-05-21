package com.mz.jarboot.core.advisor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public class TransformerManager {
    private ClassFileTransformer classFileTransformer;

    public TransformerManager(Instrumentation inst) {
        classFileTransformer = (loader, className, classBeingRedefined,
                                protectionDomain, classfileBuffer) -> {
            //待实现的扩展功能
            return classfileBuffer;
        };
        inst.addTransformer(classFileTransformer);
    }
}
