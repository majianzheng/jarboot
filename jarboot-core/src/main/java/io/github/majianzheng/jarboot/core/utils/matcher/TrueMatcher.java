package io.github.majianzheng.jarboot.core.utils.matcher;

/**
 * @author majianzheng
 * 以下代码来自开源项目Arthas
 */
public final class TrueMatcher<T> implements Matcher<T> {

    /**
     * always return true
     * @param target
     * @return
     */
    @Override
    public boolean matching(T target) {
        return true;
    }

}