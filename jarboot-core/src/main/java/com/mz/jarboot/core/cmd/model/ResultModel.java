package com.mz.jarboot.core.cmd.model;

import com.mz.jarboot.core.constant.CoreConstant;

public abstract class ResultModel {
    /**
     * 页面id
     * @return
     */
    public String getId() {
        return CoreConstant.EMPTY_STRING;
    }

    /**
     * 命令的名字
     * @return
     */
    public abstract String getName();
}
