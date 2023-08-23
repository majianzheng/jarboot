package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.core.cmd.model.HeapDumpModel;

/**
 * @author majianzheng
 */
public class HeapDumpView implements ResultView<HeapDumpModel> {
    @Override
    public String render(CommandSession session, HeapDumpModel model) {
        return JsonUtils.toJsonString(model);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
