package com.mz.jarboot.core.cmd.express;

/**
 * ExpressFactory
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ExpressFactory {

    private static final ThreadLocal<com.mz.jarboot.core.cmd.express.Express> expressRef = new ThreadLocal<com.mz.jarboot.core.cmd.express.Express>() {
        @Override
        protected com.mz.jarboot.core.cmd.express.Express initialValue() {
            return new com.mz.jarboot.core.cmd.express.OgnlExpress();
        }
    };

    /**
     * get ThreadLocal Express Object
     * @param object
     * @return
     */
    public static com.mz.jarboot.core.cmd.express.Express threadLocalExpress(Object object) {
        return expressRef.get().reset().bind(object);
    }

    public static com.mz.jarboot.core.cmd.express.Express unpooledExpress(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        return new com.mz.jarboot.core.cmd.express.OgnlExpress(new com.mz.jarboot.core.cmd.express.ClassLoaderClassResolver(classloader));
    }
}