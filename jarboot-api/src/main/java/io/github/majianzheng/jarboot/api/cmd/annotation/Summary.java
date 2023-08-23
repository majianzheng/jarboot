package io.github.majianzheng.jarboot.api.cmd.annotation;

/**
 * 概要说明注解
 * @author majianzheng
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
public @interface Summary {
    String value();
}
