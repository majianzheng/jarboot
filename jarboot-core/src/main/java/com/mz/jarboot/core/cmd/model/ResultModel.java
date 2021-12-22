package com.mz.jarboot.core.cmd.model;

import com.mz.jarboot.core.utils.StringUtils;

/**
 * @author majianzheng
 */
@SuppressWarnings("all")
public abstract class ResultModel {
    /**
     * 页面id
     * @return
     */
    public String getId() {
        return StringUtils.EMPTY;
    }

    /**
     * 命令的名字
     * @return
     */
    public abstract String getName();
}
