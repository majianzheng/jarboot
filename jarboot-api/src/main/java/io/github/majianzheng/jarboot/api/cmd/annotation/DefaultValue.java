package io.github.majianzheng.jarboot.api.cmd.annotation;

/**
 * 命令参数默认值
 * @author majianzheng
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
public @interface DefaultValue {
    String value();
}
