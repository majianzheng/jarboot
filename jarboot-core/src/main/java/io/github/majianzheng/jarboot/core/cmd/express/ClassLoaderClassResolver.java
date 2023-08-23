package io.github.majianzheng.jarboot.core.cmd.express;

import ognl.ClassResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ClassLoaderClassResolver implements ClassResolver {

    private ClassLoader classLoader;

    private Map<String, Class<?>> classes = new ConcurrentHashMap<>(101);

    public ClassLoaderClassResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class<?> classForName(String className, @SuppressWarnings("rawtypes") Map context)
                    throws ClassNotFoundException {
        Class<?> result = null;

        if ((result = classes.get(className)) == null) {
            try {
                result = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                final char dot = '.';
                if (className.indexOf(dot) == -1) {
                    result = Class.forName("java.lang." + className);
                    classes.put("java.lang." + className, result);
                }
            }
            if (result == null) {
                return null;
            }
            classes.put(className, result);
        }
        return result;
    }

}
