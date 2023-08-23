package io.github.majianzheng.jarboot.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jarboot全局选项
 * @author majianzheng
 * 如下代码基于开源项目Arthas适配修改
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {

    /*
     * 选项级别，数字越小级别越高
     */
    int level();

    /*
     * 选项名称
     */
    String name();

    /*
     * 选项摘要说明
     */
    String summary();

    /*
     * 命令描述
     */
    String description();

}