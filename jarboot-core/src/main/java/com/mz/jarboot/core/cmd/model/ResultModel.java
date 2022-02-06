package com.mz.jarboot.core.cmd.model;

import com.mz.jarboot.common.utils.StringUtils;

/**
 * @author majianzheng
 */
@SuppressWarnings("squid:S1610")
public abstract class ResultModel {
    /**
     * 页面id
     * @return id
     */
    public String getId() {
        return StringUtils.EMPTY;
    }

    /**
     * 命令的名字
     * @return name
     */
    public abstract String getName();
}
