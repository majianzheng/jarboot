package com.mz.jarboot.core.utils;

import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.Set;

/**
 * 
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("all")
public class InstrumentationUtils {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);

    public static void retransformClasses(Instrumentation inst, ClassFileTransformer transformer,
            Set<Class<?>> classes) {
        try {
            inst.addTransformer(transformer, true);

            for (Class<?> clazz : classes) {
                if (ClassUtils.isLambdaClass(clazz)) {
                    logger.info(
                            "ignore lambda class: {}, because jdk do not support retransform lambda class: https://github.com/alibaba/arthas/issues/1512.",
                            clazz.getName());
                    continue;
                }
                try {
                    inst.retransformClasses(clazz);
                } catch (Throwable e) {
                    String errorMsg = "retransformClasses class error, name: " + clazz.getName();
                    logger.error(errorMsg, e);
                }
            }
        } finally {
            inst.removeTransformer(transformer);
        }
    }

    public static void trigerRetransformClasses(Instrumentation inst, Collection<String> classes) {
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (classes.contains(clazz.getName())) {
                try {
                    inst.retransformClasses(clazz);
                } catch (Throwable e) {
                    String errorMsg = "retransformClasses class error, name: " + clazz.getName();
                    logger.error(errorMsg, e);
                }
            }
        }
    }
    private InstrumentationUtils() {}
}
