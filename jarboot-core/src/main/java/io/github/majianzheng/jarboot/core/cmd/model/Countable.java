package io.github.majianzheng.jarboot.core.cmd.model;

/**
 * Item countable for ResultModel
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public interface Countable {

    /**
     * Get item size of this result model, the value of size is greater than or equal to 1
     * @return item size of this result model
     */
    int size();

}
