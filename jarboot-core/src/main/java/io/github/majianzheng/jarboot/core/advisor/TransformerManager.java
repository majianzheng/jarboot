package io.github.majianzheng.jarboot.core.advisor;

import io.github.majianzheng.jarboot.core.utils.LogUtils;
import org.slf4j.Logger;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Java Transformer管理
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class TransformerManager {
    private static final Logger logger = LogUtils.getLogger();
    private ClassFileTransformer classFileTransformer;
    private List<ClassFileTransformer> watchTransformers = new CopyOnWriteArrayList<>();
    private List<ClassFileTransformer> traceTransformers = new CopyOnWriteArrayList<>();
    private Instrumentation instrumentation;
    private List<ClassFileTransformer> reTransformers = new CopyOnWriteArrayList<>();

    public TransformerManager(Instrumentation inst) {
        instrumentation = inst;
        init();
    }
    private void init() {
        classFileTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                for (ClassFileTransformer transformer : reTransformers) {
                    byte[] transformResult = transformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                for (ClassFileTransformer transformer : watchTransformers) {
                    byte[] transformResult = transformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                for (ClassFileTransformer transformer : traceTransformers) {
                    byte[] transformResult = transformer.transform(loader, className, classBeingRedefined,
                            protectionDomain, classfileBuffer);
                    if (transformResult != null) {
                        classfileBuffer = transformResult;
                    }
                }

                return classfileBuffer;
            }
        };

        instrumentation.addTransformer(classFileTransformer, true);
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
