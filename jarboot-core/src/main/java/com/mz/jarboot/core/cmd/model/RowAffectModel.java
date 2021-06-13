package com.mz.jarboot.core.cmd.model;

import com.mz.jarboot.core.utils.affect.RowAffect;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class RowAffectModel extends ResultModel {
    private RowAffect affect;

    public RowAffectModel() {
    }

    public RowAffectModel(RowAffect affect) {
        this.affect = affect;
    }

    @Override
    public String getName() {
        return "row_affect";
    }

    public int getRowCount() {
        return affect.rCnt();
    }

    public RowAffect affect() {
        return affect;
    }
}
