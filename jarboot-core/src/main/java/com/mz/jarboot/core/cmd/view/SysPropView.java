package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.core.cmd.model.SysPropModel;

/**
 * @author majianzheng
 */
public class SysPropView implements ResultView<SysPropModel> {
    @Override
    public String render(CommandSession session, SysPropModel model) {
        return ViewRenderUtil.renderKeyValueTable(model.getProps(), session.getCol());
    }
}
