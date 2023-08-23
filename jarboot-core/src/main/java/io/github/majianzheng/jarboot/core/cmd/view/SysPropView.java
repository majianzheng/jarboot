package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.core.cmd.model.SysPropModel;

/**
 * @author majianzheng
 */
public class SysPropView implements ResultView<SysPropModel> {
    @Override
    public String render(CommandSession session, SysPropModel model) {
        return ViewRenderUtil.renderKeyValueTable(model.getProps(), session.getCol());
    }
}
