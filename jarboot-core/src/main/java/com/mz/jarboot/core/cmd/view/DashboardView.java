package com.mz.jarboot.core.cmd.view;


import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.common.utils.JsonUtils;

/**
 * View of 'dashboard' command
 *
 * @author majianzheng
 */
public class DashboardView implements ResultView<com.mz.jarboot.core.cmd.model.DashboardModel> {

    @Override
    public String render(CommandSession session, com.mz.jarboot.core.cmd.model.DashboardModel result) {
        return JsonUtils.toJsonString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
