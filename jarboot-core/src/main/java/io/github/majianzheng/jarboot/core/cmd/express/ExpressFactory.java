package io.github.majianzheng.jarboot.core.cmd.express;

/**
 * ExpressFactory
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings({"squid:S4065", "squid:S1118", "squid:S5164"})
public class ExpressFactory {

    private static final ThreadLocal<Express> expressRef = new ThreadLocal<Express>() {
        @Override
        protected Express initialValue() {
            return new OgnlExpress();
        }
    };

    /**
     * get ThreadLocal Express Object
     * @param object obj
     * @return express
     */
    public static Express threadLocalExpress(Object object) {
        return expressRef.get().reset().bind(object);
    }

    public static Express unpooledExpress(ClassLoader classloader) {
        if (classloader == null) {
            classloader = ClassLoader.getSystemClassLoader();
        }
        return new OgnlExpress(new ClassLoaderClassResolver(classloader));
    }
}