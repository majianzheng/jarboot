package com.mz.jarboot.core.cmd.annotation;

/**
 * @author jianzhengma
 * 以下代码基于开源项目Arthas适配修改
 */
@SuppressWarnings("all")
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Option {
    java.lang.String NO_NAME = "\u0000";

    java.lang.String longName() default "\u0000";

    java.lang.String shortName() default "\u0000";

    java.lang.String argName() default "value";

    boolean required() default false;

    boolean acceptValue() default true;

    boolean acceptMultipleValues() default false;

    boolean flag() default false;

    boolean help() default false;

    java.lang.String[] choices() default {};
}
