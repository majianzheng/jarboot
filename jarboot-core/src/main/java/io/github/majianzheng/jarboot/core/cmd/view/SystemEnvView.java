package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.core.cmd.model.SystemEnvModel;

/**
 * @author majianzheng
 */
public class SystemEnvView implements ResultView<SystemEnvModel> {

    @Override
    public String render(CommandSession session, SystemEnvModel result) {
        return ViewRenderUtil.renderKeyValueTable(result.getEnv(), session.getCol());
    }
}
