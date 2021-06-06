package com.mz.jarboot.core.advisor;

import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TransformerManager {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private ClassFileTransformer classFileTransformer;
    private ConcurrentMap<Class<?>, ClassFileTransformer> onceTransformer = new ConcurrentHashMap<>();
    private Instrumentation instrumentation;

    public TransformerManager(Instrumentation inst) {
        instrumentation = inst;
        init();
    }
    private void init() {
        classFileTransformer = (loader, className, classBeingRedefined,
                                protectionDomain, classfileBuffer) -> {
            ClassFileTransformer transformer = onceTransformer.get(classBeingRedefined);
            if (null != transformer) {
                logger.info("classFileTransformer>>{}", className);
                byte[] buffer =transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                if (null != buffer) {
                    classfileBuffer = buffer;
                }
                onceTransformer.remove(classBeingRedefined);
            }
            //待实现的扩展功能
            return classfileBuffer;
        };
        instrumentation.addTransformer(classFileTransformer, true);
    }

    public void destroy() {
        instrumentation.removeTransformer(classFileTransformer);
    }

    public synchronized void addOnceTransformer(Class<?> cls, ClassFileTransformer transformer) {
        onceTransformer.put(cls, transformer);
    }

    public void retransformClasses(Class<?> cls) {
        try {
            logger.info("retransformClasses>>{}", cls.getName());
            instrumentation.retransformClasses(cls);
        } catch (UnmodifiableClassException e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
