package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.common.JsonUtils;
import com.mz.jarboot.core.cmd.model.HeapDumpModel;

/**
 * @author jianzhengma
 */
public class HeapDumpView implements ResultView<com.mz.jarboot.core.cmd.model.HeapDumpModel> {
    @Override
    public String render(HeapDumpModel model) {
        return JsonUtils.toJSONString(model);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
