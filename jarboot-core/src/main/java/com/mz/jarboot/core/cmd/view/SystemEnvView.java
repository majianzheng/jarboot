package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.core.cmd.model.SystemEnvModel;

/**
 * @author majianzheng
 */
public class SystemEnvView implements ResultView<SystemEnvModel> {

    @Override
    public String render(CommandSession session, SystemEnvModel result) {
        return ViewRenderUtil.renderKeyValueTable(result.getEnv(), session.getCol());
    }
}
