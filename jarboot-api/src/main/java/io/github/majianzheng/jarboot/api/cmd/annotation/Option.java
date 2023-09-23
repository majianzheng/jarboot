package io.github.majianzheng.jarboot.api.cmd.annotation;

/**
 * Option 注解
 * 如： -c classloader
 * @author majianzheng
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Option {
    String NO_NAME = "\u0000";

    String longName() default NO_NAME;

    String shortName() default NO_NAME;

    String argName() default "value";

    boolean required() default false;

    boolean acceptValue() default true;

    boolean acceptMultipleValues() default false;

    boolean flag() default false;

    boolean help() default false;

    String[] choices() default {};
}
