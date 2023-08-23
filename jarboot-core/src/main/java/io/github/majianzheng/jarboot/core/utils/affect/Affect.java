package io.github.majianzheng.jarboot.core.utils.affect;

import static java.lang.System.currentTimeMillis;

/**
 * 影响反馈
 * @author majianzheng
 * 以下代码来自开源项目Arthas
 */
public class Affect {

    private final long start = currentTimeMillis();

    /**
     * 影响耗时(ms)
     *
     * @return 获取耗时(ms)
     */
    public long cost() {
        return currentTimeMillis() - start;
    }

    @Override
    public String toString() {
        return String.format("Affect cost in %s ms.", cost());
    }
}
