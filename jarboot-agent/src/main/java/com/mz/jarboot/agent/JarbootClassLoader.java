package com.mz.jarboot.agent;

import java.net.URL;
import java.net.URLClassLoader;

public class JarbootClassLoader extends URLClassLoader {
    public JarbootClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (null != loadedClass) {
            return loadedClass;
        }
        if (null != name && (name.startsWith("sun.") || name.startsWith("java."))) {
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
