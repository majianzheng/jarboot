package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.common.JsonUtils;

/**
 * @author majianzheng
 */
public class DumpClassView implements ResultView<com.mz.jarboot.core.cmd.model.DumpClassModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.DumpClassModel result) {
        return JsonUtils.toJsonString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
