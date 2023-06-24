package com.mz.jarboot.core.cmd.view;


import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.common.utils.JsonUtils;

/**
 * @author majianzheng
 */
public class JadView implements ResultView<com.mz.jarboot.core.cmd.model.JadModel> {

    @Override
    public String render(CommandSession session, com.mz.jarboot.core.cmd.model.JadModel result) {
        return JsonUtils.toJsonString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
