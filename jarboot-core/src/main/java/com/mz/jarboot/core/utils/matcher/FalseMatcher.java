package com.mz.jarboot.core.utils.matcher;

/**
 * @author majianzheng
 * 以下代码来自开源项目Arthas
 */
public class FalseMatcher<T> implements com.mz.jarboot.core.utils.matcher.Matcher<T> {

    /**
     * always return false
     * @param target
     * @return true/false
     */
    @Override
    public boolean matching(T target) {
        return false;
    }
}
