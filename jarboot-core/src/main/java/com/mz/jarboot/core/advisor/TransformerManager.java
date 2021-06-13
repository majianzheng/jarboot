package com.mz.jarboot.core.advisor;

import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Java Transformer管理
 * @author jianzhengma
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("all")
public class TransformerManager {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private ClassFileTransformer classFileTransformer;
    private ConcurrentMap<Class<?>, ClassFileTransformer> onceTransformer = new ConcurrentHashMap<>();
    private List<ClassFileTransformer> watchTransformers = new CopyOnWriteArrayList<>();
    private List<ClassFileTransformer> traceTransformers = new CopyOnWriteArrayList<>();
    private Instrumentation instrumentation;
    private List<ClassFileTransformer> reTransformers = new CopyOnWriteArrayList<>();

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

            for (ClassFileTransformer classFileTransformer : reTransformers) {
                byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                        protectionDomain, classfileBuffer);
                if (transformResult != null) {
                    classfileBuffer = transformResult;
                }
            }

            for (ClassFileTransformer classFileTransformer : watchTransformers) {
                byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                        protectionDomain, classfileBuffer);
                if (transformResult != null) {
                    classfileBuffer = transformResult;
                }
            }

            for (ClassFileTransformer classFileTransformer : traceTransformers) {
                byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                        protectionDomain, classfileBuffer);
                if (transformResult != null) {
                    classfileBuffer = transformResult;
                }
            }

            return classfileBuffer;
        };
        instrumentation.addTransformer(classFileTransformer, true);
    }

    public synchronized void addOnceTransformer(Class<?> cls, ClassFileTransformer transformer) {
        onceTransformer.put(cls, transformer);
    }

    public void addTransformer(ClassFileTransformer transformer, boolean isTracing) {
        if (isTracing) {
            traceTransformers.add(transformer);
        } else {
            watchTransformers.add(transformer);
        }
    }

    public void addRetransformer(ClassFileTransformer transformer) {
        reTransformers.add(transformer);
    }

    public void removeTransformer(ClassFileTransformer transformer) {
        reTransformers.remove(transformer);
        watchTransformers.remove(transformer);
        traceTransformers.remove(transformer);
    }

    public void destroy() {
        reTransformers.clear();
        watchTransformers.clear();
        traceTransformers.clear();
        instrumentation.removeTransformer(classFileTransformer);
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
