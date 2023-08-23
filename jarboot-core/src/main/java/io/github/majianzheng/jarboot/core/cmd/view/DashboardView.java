package io.github.majianzheng.jarboot.core.cmd.view;


import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.core.cmd.model.DashboardModel;

/**
 * View of 'dashboard' command
 *
 * @author majianzheng
 */
public class DashboardView implements ResultView<DashboardModel> {

    @Override
    public String render(CommandSession session, DashboardModel result) {
        return JsonUtils.toJsonString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
