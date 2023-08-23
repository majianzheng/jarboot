package io.github.majianzheng.jarboot.core.utils.matcher;

/**
 * 匹配器
 * @author majianzheng
 * 以下代码来自开源项目Arthas
 */
public interface Matcher<T> {

    /**
     * 是否匹配
     *
     * @param target 目标字符串
     * @return 目标字符串是否匹配表达式
     */
    boolean matching(T target);

}
