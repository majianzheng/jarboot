package com.mz.jarboot.core.server;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

/**
 * @see ClassLoader#loadClass(String)
 * @author majianzheng
 * 以下代码来自开源项目Arthas
 */
@SuppressWarnings("all")
@Instrument(Class = "java.lang.ClassLoader")
public abstract class ClassLoader_Instrument {
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("java.jarboot.")) {
            ClassLoader extClassLoader = ClassLoader.getSystemClassLoader().getParent();
            if (extClassLoader != null) {
                return extClassLoader.loadClass(name);
            }
        }

        Class clazz = InstrumentApi.invokeOrigin();
        return clazz;
    }
}
