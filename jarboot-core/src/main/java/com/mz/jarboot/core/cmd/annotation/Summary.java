package com.mz.jarboot.core.cmd.annotation;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
public @interface Summary {
    String value();
}
