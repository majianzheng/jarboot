package com.mz.jarboot.api.cmd.annotation;

/**
 * Option 注解
 * 如： -c classloader
 * @author majianzheng
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Option {
    java.lang.String NO_NAME = "\u0000";

    java.lang.String longName() default NO_NAME;

    java.lang.String shortName() default NO_NAME;

    java.lang.String argName() default "value";

    boolean required() default false;

    boolean acceptValue() default true;

    boolean acceptMultipleValues() default false;

    boolean flag() default false;

    boolean help() default false;

    java.lang.String[] choices() default {};
}
