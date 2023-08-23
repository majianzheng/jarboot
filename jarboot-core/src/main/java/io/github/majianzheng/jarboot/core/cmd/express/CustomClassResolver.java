package io.github.majianzheng.jarboot.core.cmd.express;

import ognl.ClassResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author majianzheng
 */
public class CustomClassResolver implements ClassResolver {

    public static final CustomClassResolver CUSTOM_CLASS_RESOLVER = new CustomClassResolver();

    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>(101);

    private CustomClassResolver() {

    }

    @SuppressWarnings({"java:S3824", "PMD.UndefineMagicConstantRule"})
    @Override
    public Class<?> classForName(String className, Map context) throws ClassNotFoundException {
        Class<?> result = null;

        if ((result = classes.get(className)) == null) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader != null) {
                    result = classLoader.loadClass(className);
                } else {
                    result = Class.forName(className);
                }
            } catch (ClassNotFoundException ex) {
                if (className.indexOf('.') == -1) {
                    result = Class.forName("java.lang." + className);
                    classes.put("java.lang." + className, result);
                }
            }
            classes.put(className, result);
        }
        return result;
    }
}
