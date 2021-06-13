package com.mz.jarboot.core.cmd.annotation;

/**
 * @author jianzhengma
 * 以下代码基于开源项目Arthas适配修改
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
public @interface DefaultValue {
    java.lang.String value();
}
