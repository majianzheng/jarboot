package com.mz.jarboot.core.cmd.view;

import com.alibaba.fastjson.JSON;

/**
 * @author majianzheng
 */
public class DumpClassView implements ResultView<com.mz.jarboot.core.cmd.model.DumpClassModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.DumpClassModel result) {
        return JSON.toJSONString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
