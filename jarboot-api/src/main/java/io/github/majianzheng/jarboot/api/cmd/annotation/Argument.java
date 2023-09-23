package io.github.majianzheng.jarboot.api.cmd.annotation;

/**
 * 命令参数注解
 * @author majianzheng
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Argument {
    String argName() default "value";

    int index();

    boolean required() default true;
}
