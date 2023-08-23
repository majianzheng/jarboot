package io.github.majianzheng.jarboot.core.utils.matcher;

import io.github.majianzheng.jarboot.core.utils.JarbootCheckUtils;

/**
 * 字符串全匹配
 * @author majianzheng
 * 以下代码来自开源项目Arthas
 */
public class EqualsMatcher<T> implements Matcher<T> {

    private final T pattern;

    public EqualsMatcher(T pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matching(T target) {
        return JarbootCheckUtils.isEquals(target, pattern);
    }
}
