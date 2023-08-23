package io.github.majianzheng.jarboot.core.cmd.view;


import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.core.cmd.model.JadModel;

/**
 * @author majianzheng
 */
public class JadView implements ResultView<JadModel> {

    @Override
    public String render(CommandSession session, JadModel result) {
        return JsonUtils.toJsonString(result);
    }

    @Override
    public boolean isJson() {
        return true;
    }
}
