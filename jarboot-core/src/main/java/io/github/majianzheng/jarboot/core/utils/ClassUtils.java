package io.github.majianzheng.jarboot.core.utils;

import com.alibaba.deps.org.objectweb.asm.Type;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.cmd.model.ClassDetailVO;
import io.github.majianzheng.jarboot.core.cmd.model.ClassLoaderVO;
import io.github.majianzheng.jarboot.core.cmd.model.ClassVO;
import io.github.majianzheng.jarboot.core.cmd.model.MethodVO;
import io.github.majianzheng.jarboot.text.ui.Element;
import io.github.majianzheng.jarboot.text.ui.TableElement;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 *
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class ClassUtils {

    public static String getCodeSource(final CodeSource cs) {
        if (null == cs || null == cs.getLocation() || null == cs.getLocation().getFile()) {
            return StringUtils.EMPTY;
        }

        return cs.getLocation().getFile();
    }

    public static boolean isLambdaClass(Class<?> clazz) {
        return clazz.getName().contains("$$Lambda$");
    }

    public static TableElement renderClassInfo(ClassDetailVO clazz) {
        return renderClassInfo(clazz, false, null);
    }

    @SuppressWarnings("squid:S1192")
    public static TableElement renderClassInfo(ClassDetailVO clazz, boolean isPrintField, Integer expand) {
        TableElement table = new TableElement();

        table.row("class-info", clazz.getClassInfo())
                .row("code-source", clazz.getCodeSource())
                .row("name", clazz.getName())
                .row("isInterface", ("" + clazz.isInterface()))
                .row("isAnnotation", "" + clazz.isAnnotation())
                .row("isEnum", ("" + clazz.isEnum()))
                .row("isAnonymousClass", ("" + clazz.isAnonymousClass()))
                .row("isArray", ("" + clazz.isArray()))
                .row("isLocalClass", ("" + clazz.isLocalClass()))
                .row("isMemberClass", ("" + clazz.isMemberClass()))
                .row("isPrimitive", ("" + clazz.isPrimitive()))
                .row("isSynthetic", ("" + clazz.isSynthetic()))
                .row("simple-name", (clazz.getSimpleName()))
                .row("modifier", (clazz.getModifier()))
                .row("annotation", (StringUtils.join(clazz.getAnnotations(), ",")))
                .row("interfaces", (StringUtils.join(clazz.getInterfaces(), ",")))
                .row("super-class", RenderUtil.render(TypeRenderUtils.drawSuperClass(clazz)))
                .row("class-loader", RenderUtil.render(TypeRenderUtils.drawClassLoader(clazz)))
                .row("classLoaderHash", (clazz.getClassLoaderHash()));

        if (isPrintField) {
            table.row("fields", RenderUtil.render(TypeRenderUtils.drawField(clazz, expand)));
        }
        return table;
    }

    public static ClassDetailVO createClassInfo(Class<?> clazz, boolean withFields) {
        CodeSource cs = clazz.getProtectionDomain().getCodeSource();
        ClassDetailVO classInfo = new ClassDetailVO();
        classInfo.setName(StringUtils.classname(clazz));
        classInfo.setClassInfo(StringUtils.classname(clazz));
        classInfo.setCodeSource(ClassUtils.getCodeSource(cs));
        classInfo.setInterface(clazz.isInterface());
        classInfo.setAnnotation(clazz.isAnnotation());
        classInfo.setEnum(clazz.isEnum());
        classInfo.setAnonymousClass(clazz.isAnonymousClass());
        classInfo.setArray(clazz.isArray());
        classInfo.setLocalClass(clazz.isLocalClass());
        classInfo.setMemberClass(clazz.isMemberClass());
        classInfo.setPrimitive(clazz.isPrimitive());
        classInfo.setSynthetic(clazz.isSynthetic());
        classInfo.setSimpleName(clazz.getSimpleName());
        classInfo.setModifier(StringUtils.modifier(clazz.getModifiers(), ','));
        classInfo.setAnnotations(TypeRenderUtils.getAnnotations(clazz));
        classInfo.setInterfaces(TypeRenderUtils.getInterfaces(clazz));
        classInfo.setSuperClass(TypeRenderUtils.getSuperClass(clazz));
        classInfo.setClassloader(TypeRenderUtils.getClassloader(clazz));
        classInfo.setClassLoaderHash(StringUtils.classLoaderHash(clazz));
        if (withFields) {
            classInfo.setFields(TypeRenderUtils.getFields(clazz));
        }
        return classInfo;
    }

    public static ClassVO createSimpleClassInfo(Class<?> clazz) {
        ClassVO classInfo = new ClassVO();
        fillSimpleClassVO(clazz, classInfo);
        return classInfo;
    }

    public static void fillSimpleClassVO(Class<?> clazz, ClassVO classInfo) {
        classInfo.setName(StringUtils.classname(clazz));
        classInfo.setClassloader(TypeRenderUtils.getClassloader(clazz));
        classInfo.setClassLoaderHash(StringUtils.classLoaderHash(clazz));
    }

    public static MethodVO createMethodInfo(Method method, Class<?> clazz, boolean detail) {
        MethodVO methodVO = new MethodVO();
        methodVO.setDeclaringClass(clazz.getName());
        methodVO.setMethodName(method.getName());
        methodVO.setDescriptor(Type.getMethodDescriptor(method));
        methodVO.setConstructor(false);
        if (detail) {
            methodVO.setModifier(StringUtils.modifier(method.getModifiers(), ','));
            methodVO.setAnnotations(TypeRenderUtils.getAnnotations(method.getDeclaredAnnotations()));
            methodVO.setParameters(getClassNameList(method.getParameterTypes()));
            methodVO.setReturnType(StringUtils.classname(method.getReturnType()));
            methodVO.setExceptions(getClassNameList(method.getExceptionTypes()));
            methodVO.setClassLoaderHash(StringUtils.classLoaderHash(clazz));
        }
        return methodVO;
    }

    public static MethodVO createMethodInfo(Constructor<?> constructor, Class<?> clazz, boolean detail) {
        MethodVO methodVO = new MethodVO();
        methodVO.setDeclaringClass(clazz.getName());
        methodVO.setDescriptor(Type.getConstructorDescriptor(constructor));
        methodVO.setMethodName("<init>");
        methodVO.setConstructor(true);
        if (detail) {
            methodVO.setModifier(StringUtils.modifier(constructor.getModifiers(), ','));
            methodVO.setAnnotations(TypeRenderUtils.getAnnotations(constructor.getDeclaredAnnotations()));
            methodVO.setParameters(getClassNameList(constructor.getParameterTypes()));
            methodVO.setExceptions(getClassNameList(constructor.getExceptionTypes()));
            methodVO.setClassLoaderHash(StringUtils.classLoaderHash(clazz));
        }
        return methodVO;
    }

    public static TableElement renderMethod(MethodVO method) {
        TableElement table = new TableElement();
        table.row("declaring-class", (method.getDeclaringClass()))
                .row("method-name", (method.getMethodName()))
                .row("modifier", (method.getModifier()))
                .row("annotation", (TypeRenderUtils.drawAnnotation(method.getAnnotations())))
                .row("parameters", (TypeRenderUtils.drawParameters(method.getParameters())))
                .row("return", (method.getReturnType()))
                .row("exceptions", (TypeRenderUtils.drawExceptions(method.getExceptions())))
                .row("classLoaderHash", (method.getClassLoaderHash()));
        return table;
    }

    public static TableElement renderConstructor(MethodVO constructor) {
        TableElement table = new TableElement();
        table.row("declaring-class", (constructor.getDeclaringClass()))
                .row("constructor-name", "<init>")
                .row("modifier", (constructor.getModifier()))
                .row("annotation", (TypeRenderUtils.drawAnnotation(constructor.getAnnotations())))
                .row("parameters", (TypeRenderUtils.drawParameters(constructor.getParameters())))
                .row("exceptions", (TypeRenderUtils.drawExceptions(constructor.getExceptions())))
                .row("classLoaderHash", (constructor.getClassLoaderHash()));
        return table;
    }

    public static String[] getClassNameList(Class<?>[] classes) {
        List<String> list = new ArrayList<>();
        for (Class<?> anInterface : classes) {
            list.add(StringUtils.classname(anInterface));
        }
        return list.toArray(new String[0]);
    }

    public static List<ClassVO> createClassVOList(Collection<Class<?>> matchedClasses) {
        List<ClassVO> classVOs = new ArrayList<>(matchedClasses.size());
        for (Class<?> aClass : matchedClasses) {
            ClassVO classVO = createSimpleClassInfo(aClass);
            classVOs.add(classVO);
        }
        return classVOs;
    }

    public static ClassLoaderVO createClassLoaderVO(ClassLoader classLoader) {
        ClassLoaderVO classLoaderVO = new ClassLoaderVO();
        classLoaderVO.setHash(classLoaderHash(classLoader));
        classLoaderVO.setName(classLoader==null?"BootstrapClassLoader":classLoader.toString());
        ClassLoader parent = classLoader == null ? null : classLoader.getParent();
        classLoaderVO.setParent(parent==null?null:parent.toString());
        return classLoaderVO;
    }

    public static List<ClassLoaderVO> createClassLoaderVOList(Collection<ClassLoader> classLoaders) {
        List<ClassLoaderVO> classLoaderVOList = new ArrayList<>();
        for (ClassLoader classLoader : classLoaders) {
            classLoaderVOList.add(createClassLoaderVO(classLoader));
        }
        return classLoaderVOList;
    }

    public static String classLoaderHash(Class<?> clazz) {
        if (clazz == null || clazz.getClassLoader() == null) {
            return "null";
        }
        return Integer.toHexString(clazz.getClassLoader().hashCode());
    }

    public static String classLoaderHash(ClassLoader classLoader) {
        if (classLoader == null ) {
            return "null";
        }
        return Integer.toHexString(classLoader.hashCode());
    }

    public static Element renderMatchedClasses(Collection<ClassVO> matchedClasses) {
        TableElement table = new TableElement();
        table.row("NAME", "HASHCODE", "CLASSLOADER");

        for (ClassVO c : matchedClasses) {
            table.row(c.getName(), c.getClassLoaderHash(), RenderUtil.render(TypeRenderUtils.drawClassLoader(c)));
        }
        return table;
    }

    private ClassUtils() {}
}
