package io.github.majianzheng.jarboot.core.cmd.view;


import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.cmd.model.RowAffectModel;

/**
 * @author majianzheng
 */
public class RowAffectView implements ResultView<RowAffectModel> {
    @Override
    public String render(CommandSession session, RowAffectModel result) {
        return (result.affect() + StringUtils.LF);
    }
}
