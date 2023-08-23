package io.github.majianzheng.jarboot.core.utils;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ClassLoaderUtils {

    public static Set<ClassLoader> getAllClassLoader(Instrumentation inst) {
        Set<ClassLoader> classLoaderSet = new HashSet<>();

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null) {
                classLoaderSet.add(classLoader);
            }
        }
        return classLoaderSet;
    }

    public static ClassLoader getClassLoader(Instrumentation inst, String hashCode) {
        if (hashCode == null || hashCode.isEmpty()) {
            return null;
        }

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null &&
                    Integer.toHexString(classLoader.hashCode()).equals(hashCode)) {
                return classLoader;
            }
        }
        return null;
    }

    /**
     * 通过类名查找classloader
     * @param inst {@link Instrumentation}
     * @param classLoaderClassName classLoaderClassName
     * @return Classloader list
     */
    public static List<ClassLoader> getClassLoaderByClassName(Instrumentation inst, String classLoaderClassName) {
        List<ClassLoader> matchClassLoaders = new ArrayList<>();
        if (classLoaderClassName == null || classLoaderClassName.isEmpty()) {
            return matchClassLoaders;
        }
        Set<ClassLoader> classLoaderSet = getAllClassLoader(inst);
        for (ClassLoader classLoader : classLoaderSet) {
            if (classLoader.getClass().getName().equals(classLoaderClassName)) {
                matchClassLoaders.add(classLoader);
            }
        }
        return matchClassLoaders;
    }

    public static String classLoaderHash(ClassLoader classLoader) {
        int hashCode = 0;
        if (classLoader == null) {
            hashCode = System.identityHashCode(classLoader);
        } else {
            hashCode = classLoader.hashCode();
        }
        if (hashCode <= 0) {
            hashCode = System.identityHashCode(classLoader);
            if (hashCode < 0) {
                hashCode = hashCode & Integer.MAX_VALUE;
            }
        }
        return Integer.toHexString(hashCode);
    }

    private ClassLoaderUtils() {}
}
