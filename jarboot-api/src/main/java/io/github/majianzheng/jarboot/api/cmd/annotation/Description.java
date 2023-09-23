package io.github.majianzheng.jarboot.api.cmd.annotation;

/**
 * 命令、参数描述
 * @author majianzheng
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
public @interface Description {
    String value();
}
