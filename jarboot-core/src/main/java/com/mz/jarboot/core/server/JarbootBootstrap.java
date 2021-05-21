package com.mz.jarboot.core.server;

import com.mz.jarboot.core.advisor.TransformerManager;

import java.lang.instrument.Instrumentation;
import java.util.Base64;

public class JarbootBootstrap {
    private static JarbootBootstrap bootstrap;
    private TransformerManager transformerManager;

    private JarbootBootstrap(Instrumentation inst, String args) {
        transformerManager = new TransformerManager(inst);
        if (null == args || args.isEmpty()) {
            return;
        }
        //解析args，获取目标服务端口
        String url = new String(Base64.getDecoder().decode(args));
    }
    public synchronized static JarbootBootstrap getInstance(Instrumentation inst, String args) {
        if (bootstrap != null) {
            return bootstrap;
        }
        bootstrap = new JarbootBootstrap(inst, args);
        return bootstrap;
    }
    public static JarbootBootstrap getInstance() {
        if (null == bootstrap) {
            throw new IllegalStateException("Jarboot must be initialized before!");
        }
        return bootstrap;
    }
}
