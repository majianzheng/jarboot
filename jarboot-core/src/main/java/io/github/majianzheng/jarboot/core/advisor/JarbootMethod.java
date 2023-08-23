package io.github.majianzheng.jarboot.core.advisor;

import com.alibaba.deps.org.objectweb.asm.Type;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 主要用于 tt 命令重放使用
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class JarbootMethod {
    private final Class<?> clazz;
    private final String methodName;
    private final String methodDesc;

    private Constructor<?> constructor;
    private Method method;

    @SuppressWarnings({"java:S1181", "PMD.UndefineMagicConstantRule"})
    private void initMethod() {
        if (constructor != null || method != null) {
            return;
        }

        try {
            ClassLoader loader = this.clazz.getClassLoader();
            final Type asmType = Type.getMethodType(methodDesc);

            // to arg types
            final Class<?>[] argsClasses = new Class<?>[asmType.getArgumentTypes().length];
            for (int index = 0; index < argsClasses.length; index++) {
                // asm class descriptor to jvm class
                final Class<?> argumentClass;
                final Type argumentAsmType = asmType.getArgumentTypes()[index];
                switch (argumentAsmType.getSort()) {
                case Type.BOOLEAN:
                    argumentClass = boolean.class;
                    break;
                case Type.CHAR:
                    argumentClass = char.class;
                    break;
                case Type.BYTE:
                    argumentClass = byte.class;
                    break;
                case Type.SHORT:
                    argumentClass = short.class;
                    break;
                case Type.INT:
                    argumentClass = int.class;
                    break;
                case Type.FLOAT:
                    argumentClass = float.class;
                    break;
                case Type.LONG:
                    argumentClass = long.class;
                    break;
                case Type.DOUBLE:
                    argumentClass = double.class;
                    break;
                case Type.ARRAY:
                    argumentClass = toClass(loader, argumentAsmType.getInternalName());
                    break;
                case Type.VOID:
                    argumentClass = void.class;
                    break;
                case Type.OBJECT:
                case Type.METHOD:
                default:
                    argumentClass = toClass(loader, argumentAsmType.getClassName());
                    break;
                }

                argsClasses[index] = argumentClass;
            }

            if ("<init>".equals(this.methodName)) {
                this.constructor = clazz.getDeclaredConstructor(argsClasses);
            } else {
                this.method = clazz.getDeclaredMethod(methodName, argsClasses);
            }
        } catch (Throwable e) {
            throw new JarbootRunException(e);
        }

    }

    private Class<?> toClass(ClassLoader loader, String className) throws ClassNotFoundException {
        return Class.forName(StringUtils.normalizeClassName(className), true, toClassLoader(loader));
    }

    private ClassLoader toClassLoader(ClassLoader loader) {
        return null != loader ? loader : JarbootMethod.class.getClassLoader();
    }

    /**
     * 获取方法名称
     *
     * @return 返回方法名称
     */
    public String getName() {
        return this.methodName;
    }

    @Override
    public String toString() {
        initMethod();
        if (constructor != null) {
            return constructor.toString();
        } else if (method != null) {
            return method.toString();
        }
        return "ERROR_METHOD";
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public boolean isAccessible() {
        initMethod();
        if (this.method != null) {
            return method.isAccessible();
        } else if (this.constructor != null) {
            return constructor.isAccessible();
        }
        return false;
    }

    public void setAccessible(boolean accessFlag) {
        initMethod();
        if (constructor != null) {
            constructor.setAccessible(accessFlag);
        } else if (method != null) {
            method.setAccessible(accessFlag);
        }
    }

    public Object invoke(Object target, Object... args)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        initMethod();
        if (method != null) {
            return method.invoke(target, args);
        } else if (this.constructor != null) {
            return constructor.newInstance(args);
        }
        return null;
    }

    public JarbootMethod(Class<?> clazz, String methodName, String methodDesc) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }
}
