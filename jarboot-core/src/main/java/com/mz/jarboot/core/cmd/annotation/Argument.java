package com.mz.jarboot.core.cmd.annotation;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Argument {
    java.lang.String argName() default "value";

    int index();

    boolean required() default true;
}
