package com.mz.jarboot.core.utils.matcher;

/**
 * regex matcher
 * @author majianzheng
 * 以下代码来自开源项目Arthas
 */
public class RegexMatcher implements com.mz.jarboot.core.utils.matcher.Matcher<String> {

    private final String pattern;

    public RegexMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matching(String target) {
        return null != target
                && null != pattern
                && target.matches(pattern);
    }
}