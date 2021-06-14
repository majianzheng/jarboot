package com.mz.jarboot.core.cmd.view;

import com.alibaba.fastjson.JSON;

/**
 * View of 'dashboard' command
 *
 * @author majianzheng
 */
public class DashboardView implements ResultView<com.mz.jarboot.core.cmd.model.DashboardModel> {

    @Override
    public String render(com.mz.jarboot.core.cmd.model.DashboardModel result) {
        return JSON.toJSONString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
