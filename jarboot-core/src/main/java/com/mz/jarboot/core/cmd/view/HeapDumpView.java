package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.common.utils.JsonUtils;
import com.mz.jarboot.core.cmd.model.HeapDumpModel;

/**
 * @author majianzheng
 */
public class HeapDumpView implements ResultView<com.mz.jarboot.core.cmd.model.HeapDumpModel> {
    @Override
    public String render(CommandSession session, HeapDumpModel model) {
        return JsonUtils.toJsonString(model);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
