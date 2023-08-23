package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.core.basic.EnvironmentContext;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Option;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.ClassLoaderUtils;
import io.github.majianzheng.jarboot.core.utils.ClassUtils;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import io.github.majianzheng.jarboot.core.utils.ResultUtils;
import io.github.majianzheng.jarboot.core.utils.affect.RowAffect;
import io.github.majianzheng.jarboot.core.cmd.model.*;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@Name("classloader")
@Summary("Show classloader info")
@Description(CoreConstant.EXAMPLE +
        "  classloader\n" +
        "  classloader -t\n" +
        "  classloader -l\n" +
        "  classloader -c 327a647b\n" +
        "  classloader -c 327a647b -r META-INF/MANIFEST.MF\n" +
        "  classloader -a\n" +
        "  classloader -a -c 327a647b\n" +
        "  classloader -c 659e0bfd --load demo.MathGame\n" +
        CoreConstant.WIKI + CoreConstant.WIKI_HOME + "classloader")
public class ClassLoaderCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();
    private boolean isTree = false;
    private String hashCode;
    private String classLoaderClass;
    private boolean all = false;
    private String resource;
    private boolean includeReflectionClassLoader = true;
    private boolean listClassLoader = false;

    private String loadClass = null;

    private volatile boolean isInterrupted = false;

    @Option(shortName = "t", longName = "tree", flag = true)
    @Description("Display ClassLoader tree")
    public void setTree(boolean tree) {
        isTree = tree;
    }
    
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(shortName = "a", longName = "all", flag = true)
    @Description("Display all classes loaded by ClassLoader")
    public void setAll(boolean all) {
        this.all = all;
    }

    @Option(shortName = "r", longName = "resource")
    @Description("Use ClassLoader to find resources, won't work without -c specified")
    public void setResource(String resource) {
        this.resource = resource;
    }

    @Option(shortName = "i", longName = "include-reflection-classloader", flag = true)
    @Description("Include sun.reflect.DelegatingClassLoader")
    public void setIncludeReflectionClassLoader(boolean includeReflectionClassLoader) {
        this.includeReflectionClassLoader = includeReflectionClassLoader;
    }

    @Option(shortName = "l", longName = "list-classloader", flag = true)
    @Description("Display statistics info by classloader instance")
    public void setListClassLoader(boolean listClassLoader) {
        this.listClassLoader = listClassLoader;
    }

    @Option(longName = "load")
    @Description("Use ClassLoader to load class, won't work without -c specified")
    public void setLoadClass(String className) {
        this.loadClass = className;
    }

    @Override
    public void cancel() {
        this.isInterrupted = true;
        super.cancel();
    }

    @Override
    @SuppressWarnings("java:S3776")
    public void run() {
        // ctrl-C support
        ClassLoader targetClassLoader = null;
        boolean classLoaderSpecified = false;

        Instrumentation inst = EnvironmentContext.getInstrumentation();
        
        if (hashCode != null || classLoaderClass != null) {
            classLoaderSpecified = true;
        }
        
        if (hashCode != null) {
            Set<ClassLoader> allClassLoader = getAllClassLoaders(inst);
            for (ClassLoader cl : allClassLoader) {
                if (Integer.toHexString(cl.hashCode()).equals(hashCode)) {
                    targetClassLoader = cl;
                    break;
                }
            }
        } else if (classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                targetClassLoader = matchedClassLoaders.get(0);
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                ClassLoaderModel classloaderModel = new ClassLoaderModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                session.appendResult(classloaderModel);
                session.end(false, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                session.end(false, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        if (all) {
            processAllClasses(inst);
        } else if (classLoaderSpecified && resource != null) {
            processResources(targetClassLoader);
        } else if (classLoaderSpecified && this.loadClass != null) {
            processLoadClass(targetClassLoader);
        } else if (classLoaderSpecified) {
            processClassLoader(targetClassLoader);
        } else if (listClassLoader || isTree){
            processClassLoaders(inst);
        } else {
            processClassLoaderStats(inst);
        }
    }

    /**
     * Calculate classloader statistics.
     * e.g. In JVM, there are 100 GrooyClassLoader instances, which loaded 200 classes in total
     * @param inst {@link Instrumentation}
     */
    private void processClassLoaderStats(Instrumentation inst) {
        RowAffect affect = new RowAffect();
        List<ClassLoaderInfo> classLoaderInfos = getAllClassLoaderInfo(inst);
        Map<String, ClassLoaderStat> classLoaderStats = new HashMap<>(16);
        for (ClassLoaderInfo info: classLoaderInfos) {
            String name = info.classLoader == null ? "BootstrapClassLoader" : info.classLoader.getClass().getName();
            ClassLoaderStat stat = classLoaderStats.computeIfAbsent(name, k -> new ClassLoaderStat());
            stat.addLoadedCount(info.loadedClassCount);
            stat.addNumberOfInstance(1);
        }

        // sort the map by value
        TreeMap<String, ClassLoaderStat> sorted =
                new TreeMap<>(new ValueComparator(classLoaderStats));
        sorted.putAll(classLoaderStats);
        session.appendResult(new ClassLoaderModel().setClassLoaderStats(sorted));

        affect.rCnt(sorted.keySet().size());
        session.appendResult(new RowAffectModel(affect));
        session.end();
    }

    private void processClassLoaders(Instrumentation inst) {
        RowAffect affect = new RowAffect();
        List<ClassLoaderInfo> classLoaderInfos = includeReflectionClassLoader ? getAllClassLoaderInfo(inst) :
                getAllClassLoaderInfo(inst, new SunReflectionClassLoaderFilter());

        List<ClassLoaderVO> classLoaderVOs = new ArrayList<>(classLoaderInfos.size());
        for (ClassLoaderInfo classLoaderInfo : classLoaderInfos) {
            ClassLoaderVO classLoaderVO = ClassUtils.createClassLoaderVO(classLoaderInfo.classLoader);
            classLoaderVO.setLoadedCount(classLoaderInfo.loadedClassCount());
            classLoaderVOs.add(classLoaderVO);
        }
        if (isTree){
            classLoaderVOs = processClassLoaderTree(classLoaderVOs);
        }
        session.appendResult(new ClassLoaderModel().setClassLoaders(classLoaderVOs).setTree(isTree));

        affect.rCnt(classLoaderInfos.size());
        session.appendResult(new RowAffectModel(affect));
        session.end();
    }

    private void processClassLoader(ClassLoader targetClassLoader) {
        // 根据 ClassLoader 来打印URLClassLoader的urls
        RowAffect affect = new RowAffect();
        if (targetClassLoader != null) {
            if (targetClassLoader instanceof URLClassLoader) {
                List<String> classLoaderUrls = getClassLoaderUrls(targetClassLoader);
                affect.rCnt(classLoaderUrls.size());
                if (classLoaderUrls.isEmpty()) {
                    session.console("urls is empty.");
                } else {
                    session.appendResult(new ClassLoaderModel().setUrls(classLoaderUrls));
                    affect.rCnt(classLoaderUrls.size());
                }
            } else {
                session.console("not a URLClassLoader.");
            }
        }
        session.appendResult(new RowAffectModel(affect));
        session.end();
    }

    private void processResources(ClassLoader targetClassLoader) {
        RowAffect affect = new RowAffect();
        int rowCount = 0;
        List<String> resources = new ArrayList<>();
        if (targetClassLoader != null) {
            try {
                Enumeration<URL> urls = targetClassLoader.getResources(resource);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    resources.add(url.toString());
                    rowCount++;
                }
            } catch (Exception e) {
                logger.warn("get resource failed, resource: {}", resource, e);
            }
        }
        affect.rCnt(rowCount);

        session.appendResult(new ClassLoaderModel().setResources(resources));
        session.appendResult(new RowAffectModel(affect));
        session.end();
    }

    @SuppressWarnings("java:S1181")
    private void processLoadClass(ClassLoader targetClassLoader) {
        if (targetClassLoader != null) {
            try {
                Class<?> clazz = targetClassLoader.loadClass(this.loadClass);
                session.console("load class success.");
                ClassDetailVO classInfo = ClassUtils.createClassInfo(clazz, false);
                session.appendResult(new ClassLoaderModel().setLoadClass(classInfo));

            } catch (Throwable e) {
                logger.warn("load class error, class: {}", this.loadClass, e);
                session.end(false, "load class error, class: "+this.loadClass+", error: "+e.toString());
                return;
            }
        }
        session.end();
    }

    private void processAllClasses(Instrumentation inst) {
        RowAffect affect = new RowAffect();
        getAllClasses(hashCode, inst, affect);
        if (checkInterrupted()) {
            return;
        }
        session.appendResult(new RowAffectModel(affect));
        session.end();
    }

    /**
     * 获取到所有的class, 还有它们的classloader，按classloader归类好，统一输出每个classloader里有哪些class
     * <p>
     * 当hashCode是null，则把所有的classloader的都打印
     *
     */
    @SuppressWarnings({"rawtypes", "java:S135"})
    private void getAllClasses(String hashCode, Instrumentation inst, RowAffect affect) {
        int hashCodeInt = -1;
        if (hashCode != null) {
            hashCodeInt = Integer.valueOf(hashCode, 16);
        }

        SortedSet<Class<?>> bootstrapClassSet = new TreeSet<>((Comparator<Class>) (o1, o2) -> o1.getName().compareTo(o2.getName()));

        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        Map<ClassLoader, SortedSet<Class<?>>> classLoaderClassMap = new HashMap<>(16);
        for (Class clazz : allLoadedClasses) {
            ClassLoader classLoader = clazz.getClassLoader();
            // Class loaded by BootstrapClassLoader
            if (classLoader == null) {
                if (hashCode == null) {
                    bootstrapClassSet.add(clazz);
                }
                continue;
            }

            if (hashCode != null && classLoader.hashCode() != hashCodeInt) {
                continue;
            }

            SortedSet<Class<?>> classSet = classLoaderClassMap
                    .computeIfAbsent(classLoader, k -> new TreeSet<>((o1, o2) -> o1.getName().compareTo(o2.getName())));
            classSet.add(clazz);
        }

        // output bootstrapClassSet
        int pageSize = 256;
        processClassSet(ClassUtils.createClassLoaderVO(null), bootstrapClassSet, pageSize, affect);

        // output other classSet
        for (Entry<ClassLoader, SortedSet<Class<?>>> entry : classLoaderClassMap.entrySet()) {
            if (checkInterrupted()) {
                return;
            }
            ClassLoader classLoader = entry.getKey();
            SortedSet<Class<?>> classSet = entry.getValue();
            processClassSet(ClassUtils.createClassLoaderVO(classLoader), classSet, pageSize, affect);
        }
    }

    private void processClassSet(final ClassLoaderVO classLoaderVO, Collection<Class<?>> classes, int pageSize, final RowAffect affect) {
        //分批输出classNames, Ctrl+C可以中断执行
        ResultUtils.processClassNames(classes, pageSize, (classNames, segment) -> {
            session.appendResult(new ClassLoaderModel().setClassSet(new ClassSetVO(classLoaderVO, classNames, segment)));
            affect.rCnt(classNames.size());
            return !checkInterrupted();
        });
    }

    private boolean checkInterrupted() {
        if (!session.isRunning()) {
            return true;
        }
        if(isInterrupted){
            session.end(false, "Processing has been interrupted");
            return true;
        } else {
            return false;
        }
    }

    private static List<String> getClassLoaderUrls(ClassLoader classLoader) {
        List<String> urlStrs = new ArrayList<>();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader cl = (URLClassLoader) classLoader;
            URL[] urls = cl.getURLs();
            if (urls != null) {
                for (URL url : urls) {
                    urlStrs.add(url.toString());
                }
            }
        }
        return urlStrs;
    }

    private static List<ClassLoaderVO> processClassLoaderTree(List<ClassLoaderVO> classLoaders) {
        // 以树状列出ClassLoader的继承结构
        List<ClassLoaderVO> rootClassLoaders = new ArrayList<>();
        List<ClassLoaderVO> parentNotNullClassLoaders = new ArrayList<>();
        for (ClassLoaderVO classLoaderVO : classLoaders) {
            if (classLoaderVO.getParent() == null) {
                rootClassLoaders.add(classLoaderVO);
            } else {
                parentNotNullClassLoaders.add(classLoaderVO);
            }
        }

        for (ClassLoaderVO classLoaderVO : rootClassLoaders) {
            buildTree(classLoaderVO, parentNotNullClassLoaders);
        }
        return rootClassLoaders;
    }

    private static void buildTree(ClassLoaderVO parent, List<ClassLoaderVO> parentNotNullClassLoaders) {
        for (ClassLoaderVO classLoaderVO : parentNotNullClassLoaders) {
            if (parent.getName().equals(classLoaderVO.getParent())){
                parent.addChild(classLoaderVO);
                buildTree(classLoaderVO, parentNotNullClassLoaders);
            }
        }
    }

    private static Set<ClassLoader> getAllClassLoaders(Instrumentation inst, Filter... filters) {
        Set<ClassLoader> classLoaderSet = new HashSet<>();

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader != null && shouldInclude(classLoader, filters)) {
                classLoaderSet.add(classLoader);
            }
        }
        return classLoaderSet;
    }

    @SuppressWarnings("java:S3776")
    private static List<ClassLoaderInfo> getAllClassLoaderInfo(Instrumentation inst, Filter... filters) {
        // 这里认为class.getClassLoader()返回是null的是由BootstrapClassLoader加载的，特殊处理
        ClassLoaderInfo bootstrapInfo = new ClassLoaderInfo(null);

        Map<ClassLoader, ClassLoaderInfo> loaderInfos = new HashMap<>(16);

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader == null) {
                bootstrapInfo.increase();
            } else {
                if (shouldInclude(classLoader, filters)) {
                    ClassLoaderInfo loaderInfo = loaderInfos.get(classLoader);
                    if (loaderInfo == null) {
                        loaderInfo = new ClassLoaderInfo(classLoader);
                        loaderInfos.put(classLoader, loaderInfo);
                        ClassLoader parent = classLoader.getParent();
                        while (parent != null) {
                            ClassLoaderInfo parentLoaderInfo = loaderInfos.get(parent);
                            if (parentLoaderInfo == null) {
                                parentLoaderInfo = new ClassLoaderInfo(parent);
                                loaderInfos.put(parent, parentLoaderInfo);
                            }
                            parent = parent.getParent();
                        }
                    }
                    loaderInfo.increase();
                }
            }
        }

        // 排序时，把用户自己定的ClassLoader排在最前面，以sun.
        // 开头的放后面，因为sun.reflect.DelegatingClassLoader的实例太多
        List<ClassLoaderInfo> sunClassLoaderList = new ArrayList<>();

        List<ClassLoaderInfo> otherClassLoaderList = new ArrayList<>();

        for (Entry<ClassLoader, ClassLoaderInfo> entry : loaderInfos.entrySet()) {
            ClassLoader classLoader = entry.getKey();
            if (classLoader.getClass().getName().startsWith("sun.")) {
                sunClassLoaderList.add(entry.getValue());
            } else {
                otherClassLoaderList.add(entry.getValue());
            }
        }

        Collections.sort(sunClassLoaderList);
        Collections.sort(otherClassLoaderList);

        List<ClassLoaderInfo> result = new ArrayList<>();
        result.add(bootstrapInfo);
        result.addAll(otherClassLoaderList);
        result.addAll(sunClassLoaderList);
        return result;
    }

    private static boolean shouldInclude(ClassLoader classLoader, Filter... filters) {
        if (filters == null) {
            return true;
        }

        for (Filter filter : filters) {
            if (!filter.accept(classLoader)) {
                return false;
            }
        }
        return true;
    }

    private static class ClassLoaderInfo implements Comparable<ClassLoaderInfo> {
        private final ClassLoader classLoader;
        private int loadedClassCount = 0;

        ClassLoaderInfo(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public String getName() {
            if (classLoader != null) {
                return classLoader.toString();
            }
            return "BootstrapClassLoader";
        }

        String hashCodeStr() {
            if (classLoader != null) {
                return "" + Integer.toHexString(classLoader.hashCode());
            }
            return "null";
        }

        void increase() {
            loadedClassCount++;
        }

        int loadedClassCount() {
            return loadedClassCount;
        }

        ClassLoader parent() {
            return classLoader == null ? null : classLoader.getParent();
        }

        String parentStr() {
            if (classLoader == null) {
                return "null";
            }
            ClassLoader parent = classLoader.getParent();
            if (parent == null) {
                return "null";
            }
            return parent.toString();
        }

        @SuppressWarnings("java:S1210")
        @Override
        public int compareTo(ClassLoaderInfo other) {
            if (other == null) {
                return -1;
            }
            if (other.classLoader == null) {
                return -1;
            }
            if (this.classLoader == null) {
                return -1;
            }

            return this.classLoader.getClass().getName().compareTo(other.classLoader.getClass().getName());
        }

    }

    private interface Filter {
        /**
         * accept
         * @param classLoader classloader
         * @return boolean
         */
        boolean accept(ClassLoader classLoader);
    }

    private static class SunReflectionClassLoaderFilter implements Filter {
        private static final List<String> REFLECTION_CLASSLOADERS = Arrays.asList("sun.reflect.DelegatingClassLoader",
                "jdk.internal.reflect.DelegatingClassLoader");

        @Override
        public boolean accept(ClassLoader classLoader) {
            return !REFLECTION_CLASSLOADERS.contains(classLoader.getClass().getName());
        }
    }

    public static class ClassLoaderStat {
        private int loadedCount;
        private int numberOfInstance;

        void addLoadedCount(int count) {
            this.loadedCount += count;
        }

        void addNumberOfInstance(int count) {
            this.numberOfInstance += count;
        }

        public int getLoadedCount() {
            return loadedCount;
        }

        public int getNumberOfInstance() {
            return numberOfInstance;
        }
    }

    private static class ValueComparator implements Comparator<String> {

        private Map<String, ClassLoaderStat> unsortedStats;

        ValueComparator(Map<String, ClassLoaderStat> stats) {
            this.unsortedStats = stats;
        }

        @Override
        public int compare(String o1, String o2) {
            if (null == unsortedStats) {
                return -1;
            }
            if (!unsortedStats.containsKey(o1)) {
                return 1;
            }
            if (!unsortedStats.containsKey(o2)) {
                return -1;
            }
            return unsortedStats.get(o2).getLoadedCount() - unsortedStats.get(o1).getLoadedCount();
        }
    }
}
