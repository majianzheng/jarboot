package io.github.majianzheng.jarboot.agent;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author majianzheng
 */
public class JarbootClassLoader extends URLClassLoader {
    private static final String SUN_PREFIX = "sun.";
    private static final String JAVA_PREFIX = "java.";
    private static final String API_PREFIX = "io.github.majianzheng.jarboot.api.";

    public JarbootClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (null != loadedClass) {
            return loadedClass;
        }
        if (null == name) {
            return null;
        }
        if (name.startsWith(API_PREFIX)) {
            return ClassLoader.getSystemClassLoader().loadClass(name);
        }
        if (name.startsWith(SUN_PREFIX) || name.startsWith(JAVA_PREFIX)) {
            return super.loadClass(name, resolve);
        }
        try {
            Class<?> cls = findClass(name);
            if (resolve) {
                resolveClass(cls);
            }
            return cls;
        } catch (Exception e) {
            //ignore
        }
        return super.loadClass(name, resolve);
    }
}
