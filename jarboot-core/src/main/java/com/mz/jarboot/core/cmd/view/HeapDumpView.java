package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.common.JSONUtils;
import com.mz.jarboot.core.cmd.model.HeapDumpModel;

public class HeapDumpView implements ResultView<com.mz.jarboot.core.cmd.model.HeapDumpModel> {
    @Override
    public String render(HeapDumpModel model) {
        return JSONUtils.toJSONString(model);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
